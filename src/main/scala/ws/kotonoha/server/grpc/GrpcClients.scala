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

import java.util.concurrent.ConcurrentHashMap

import com.google.inject.Provider
import io.grpc.{CallOptions, ManagedChannel, ManagedChannelBuilder}
import net.codingwell.scalaguice.ScalaModule
import org.eiennohito.grpc.stream.client.AClientFactory

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

class GrpcClientsImpl extends GrpcClients {
  private val cache = new ConcurrentHashMap[String, ManagedChannel]()

  def makeChannel(uri: String) = {
    var cached = cache.get(uri)
    if (cached == null) {
      val bldr = ManagedChannelBuilder.forTarget(uri)
      bldr.directExecutor()
      bldr.userAgent("Kotonotha Server/1.0")
      bldr.usePlaintext(true)
      cached = bldr.build()
      val present = cache.put(uri, cached)
      if (present != null) {
        present.shutdown()
      }
    }
    cached
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
}

class GrpcModule extends ScalaModule {
  override def configure() = {
    bind[GrpcClients].to[GrpcClientsImpl].in[javax.inject.Singleton]    
  }
}
