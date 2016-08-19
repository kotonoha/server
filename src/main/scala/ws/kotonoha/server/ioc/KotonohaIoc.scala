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

import java.lang.annotation.Annotation
import java.lang.reflect.Constructor

import com.google.inject._
import com.google.inject.name.Names
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http._
import net.liftweb.util.{Helpers, ThreadGlobal, Vendor}
import ws.kotonoha.server.actors.GlobalActorsModule
import ws.kotonoha.server.grpc.GrpcModule
import ws.kotonoha.server.ops.RepExampleModule

import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/07/11
  */
class KotonohaIoc(cfg: Config) extends AutoCloseable {
  private val rman = new ResourceManager

  val injector = {
    val stage = Stage.DEVELOPMENT
    Guice.createInjector(stage, new KotonohaMainModule(cfg, rman))
  }

  def spawn[T](implicit tag: ClassTag[T]): T = {
    injector.getInstance(tag.runtimeClass.asInstanceOf[Class[T]])
  }

  def spawnWithName[T](name: String)(implicit tag: ClassTag[T]): T = {
    val key = Key.get(tag.runtimeClass.asInstanceOf[Class[T]], Names.named(name))
    injector.getInstance(key)
  }

  def spawnWithAnn[T](ann: Annotation)(implicit tag: ClassTag[T]): T = {
    val key = Key.get(tag.runtimeClass.asInstanceOf[Class[T]], ann)
    injector.getInstance(key)
  }

  override def close() = rman.close()
}

class KotonohaMainModule(cfg: Config, rm: ResourceManager) extends ScalaModule {

  //add module to TestModule as well may be
  override def configure() = {
    bind[Config].toInstance(cfg)
    bind[Res].toInstance(rm)
    bind[LiftSession].toProvider[LiftSessionProviderInIoc]

    install(new AkkaModule())
    install(new GlobalActorsModule)
    install(new UserContextModule)
    install(new GrpcModule)
    install(new JmdictModule)
    install(new RMongoModule)
    install(new FormattingModule)
    install(new JumanModule)
    install(new RepExampleModule)
  }
}

class LiftSessionProviderInIoc extends Provider[LiftSession] {
  override def get() = {
    val x = KotonohaLiftSession.sessionForIoc.value
    if (x == null) throw new Exception("the method should be used only in Injection with Lift")
    x
  }
}

object KotonohaLiftSession {
  object sessionForIoc extends ThreadGlobal[LiftSession]()
}

object IocSupport {
  def checkIfSuitable(clz: Class[_]): Boolean = {
    val ctors = clz.getDeclaredConstructors
    ctors.exists(ctorFilter)
  }

  private val javaxInject = classOf[javax.inject.Inject]
  private val guiceInject = classOf[Inject]

  private def ctorFilter(c: Constructor[_]): Boolean = {
    c.getAnnotation(javaxInject) != null ||
    c.getAnnotation(guiceInject) != null ||
    c.getParameterTypes.length == 0
  }
}
