/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.ioc

import akka.actor.{Actor, ActorSystem, IndirectActorProducer, Props, Scheduler}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.google.inject._
import com.typesafe.config.Config
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

/**
  * @author eiennohito
  * @since 2016/07/14
  */
class AkkaModule(name: String = "k") extends ScalaModule {
  override def configure() = {
    bind[IocActors].to[IocActorsImpl]
  }

  @Provides
  @Singleton
  def akkaSystem(
    cfg: Config,
    res: Res
  ): ActorSystem = {
    val system = ActorSystem(name, cfg, cfg.getClass.getClassLoader)
    res.register(new AutoCloseable {
      override def close() = {
        import scala.concurrent.duration._
        Await.result(system.terminate(), 10.seconds)
      }
    })
    system
  }

  @Provides
  def exCont(asys: ActorSystem): ExecutionContext = asys.dispatcher

  @Provides
  def ece(asys: ActorSystem): ExecutionContextExecutor = asys.dispatcher

  @Provides
  def scheduler(asys: ActorSystem): Scheduler = asys.scheduler

  @Provides
  @Singleton
  def amat(asys: ActorSystem): ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(asys), "strm")(asys)
}

trait IocActors {
  def props[T: Manifest]: Props
  def inst[T: Manifest]: T = provider[T].get()
  def provider[T: Manifest]: Provider[T]
}

class IocActorsImpl @Inject()(inj: Injector) extends IocActors {
  private[this] val myinj = new ScalaInjector(inj)

  override def props[T: Manifest] = Props(classOf[ActorSpawner], inj, implicitly[Manifest[T]].runtimeClass)

  override def inst[T: Manifest] = myinj.instance[T]
  override def provider[T: Manifest] = {
    implicit val manifest = Manifest.classType(classOf[Provider[T]], implicitly[Manifest[T]])
    myinj.instance[Provider[T]]
  }
}

class ActorSpawner(inj: Injector, clazz: Class[_ <: Actor]) extends IndirectActorProducer {
  override def produce() = {
    inj.getInstance(clazz)
  }

  override def actorClass = clazz
}
