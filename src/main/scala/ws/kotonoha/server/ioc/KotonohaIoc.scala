/*
 * Copyright 2012-2013 eiennohito (Tolmachev Arseny)
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

import com.google.inject._
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.{LiftSession, SnippetInstantiation}
import net.liftweb.util.ThreadGlobal

/**
  * @author eiennohito
  * @since 2016/07/11
  */
class KotonohaIoc(cfg: Config) {
  val injector = {
    val stage = Stage.DEVELOPMENT
    Guice.createInjector(stage, new KotonohaMainModule(cfg))
  }
}

class KotonohaMainModule(cfg: Config) extends ScalaModule {
  override def configure() = {
    bind[Config].toInstance(cfg)
    bind[LiftSession].toProvider[LiftSessionProviderInIoc]
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

class KotonohaLiftInjector(inj: Injector) extends SnippetInstantiation {
  def checkIfSuitable(clz: Class[_]): Boolean = {
    val ctors = clz.getDeclaredConstructors
    ctors.exists(_.getAnnotation(classOf[Inject])!= null)
  }

  override def factoryFor[T](clz: Class[T]) = {
    implicit val mf = Manifest.classType[T](clz)
    if (checkIfSuitable(clz)) {
      Full(SnippetInstantiation { (pp, sess) =>
        net.liftweb.util.ControlHelpers.tryo {
          KotonohaLiftSession.sessionForIoc.doWith(sess) {
            inj.getInstance(clz)
          }
        }
      })
    } else Empty
  }
}
