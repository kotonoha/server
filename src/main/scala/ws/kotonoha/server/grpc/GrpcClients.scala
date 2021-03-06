/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.kotonoha.server.grpc

import java.util.concurrent.TimeUnit

import akka.actor.Scheduler
import com.github.benmanes.caffeine.cache.{Cache, Caffeine, RemovalCause, RemovalListener}
import com.google.inject.{Inject, Provider}
import com.typesafe.scalalogging.StrictLogging
import io.grpc.ClientCall.Listener
import io.grpc.Status.Code
import io.grpc._
import net.codingwell.scalaguice.ScalaModule
import org.eiennohito.grpc.stream.client.AClientFactory
import ws.kotonoha.server.ioc.Res

import scala.compat.java8.functionConverterImpls.AsJavaFunction
import scala.concurrent.ExecutionContextExecutor

/**
  * @author eiennohito
  * @since 2016/07/15
  */


trait ClientContainer {
  def service[T <: AClientFactory](fact: T): T#Service
}

class ClientContaierImpl(mc: Provider[ManagedChannel], copts: CallOptions) extends ClientContainer {
  override def service[T <: AClientFactory](fact: T) = {
    fact.build(mc.get(), copts)
  }
}


trait GrpcClients {
  def clientTo(uri: String): ClientContainer
}

class InvalidatingInterceptor(cache: Cache[String, ManagedChannel], uri: String) extends ClientInterceptor with StrictLogging {
  override def interceptCall[ReqT, RespT](method: MethodDescriptor[ReqT, RespT], callOptions: CallOptions, next: Channel) = {
    new ForwardingClientCall[ReqT, RespT] { call =>
      override val delegate: ClientCall[ReqT, RespT] = next.newCall(method, callOptions)
      override def start(responseListener: Listener[RespT], headers: Metadata): Unit = {
        val lstner = new ForwardingClientCallListener[RespT] {
          override val delegate: Listener[RespT] = responseListener
          override def onClose(status: Status, trailers: Metadata): Unit = {
            status.getCode match {
              case Code.INTERNAL | Code.UNIMPLEMENTED | Code.UNKNOWN =>
                val item = cache.getIfPresent(uri)
                if (item != null) {
                  logger.info(s"invalidating client for uri=$uri")
                  cache.invalidate(item)
                }
              case _ =>
            }
            super.onClose(status, trailers)
          }
        }
        super.start(lstner, headers)
      }
    }
  }

}

class GrpcClientsImpl @Inject() (
  ece: ExecutionContextExecutor,
  sch: Scheduler,
  res: Res
) extends GrpcClients with StrictLogging with AutoCloseable {

  res.register(this)

  private val listener = new RemovalListener[String, ManagedChannel] {
    override def onRemoval(key: String, item: ManagedChannel, cause: RemovalCause): Unit = {
      import scala.concurrent.duration._
      sch.scheduleOnce(15.seconds) {
        try {
          item.shutdown()
          item.awaitTermination(5, TimeUnit.SECONDS)
        } catch {
          case e: Exception =>
            logger.error(s"error when terminating grpc channel for uri=$key had an exception", e)
        }
      } (ece)
    }
  }

  private val cache = {
    Caffeine.newBuilder()
      .expireAfterAccess(15, TimeUnit.MINUTES)
      .removalListener(listener)
      .executor(ece)
      .build[String, ManagedChannel]()
  }

  def makeChannel(uri: String) = {
    cache.get(uri, new AsJavaFunction[String, ManagedChannel](s => {
      logger.info(s"creating a new channel for uri=$s")
      val bldr = ManagedChannelBuilder.forTarget(s)
      bldr.directExecutor()
      bldr.userAgent("Kotonotha Server/1.0")
      bldr.usePlaintext(true)
      bldr.intercept(new InvalidatingInterceptor(cache, s))
      bldr.build()
    }))
  }

  private val defautOpts = {
    val init = CallOptions.DEFAULT
    init.withCompression("gzip")
  }

  def clientTo(uri: String): ClientContainer = {
    val ccont = new ClientContaierImpl(new Provider[ManagedChannel] {
      override def get() = makeChannel(uri)
    }, defautOpts)
    ccont
  }

  override def close(): Unit = {
    val iter = cache.asMap().entrySet().iterator()
    while (iter.hasNext) {
      iter.next().getValue.shutdownNow()
    }
  }
}

class GrpcModule extends ScalaModule {
  override def configure() = {
    bind[GrpcClients].to[GrpcClientsImpl].in[javax.inject.Singleton]
  }
}
