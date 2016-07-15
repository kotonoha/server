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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.google.inject.{Provides, Singleton}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

/**
  * @author eiennohito
  * @since 2016/07/14
  */
class AkkaModule(name: String = "k") extends ScalaModule{
  override def configure() = {}

  @Provides
  @Singleton
  def akkaSystem(
    cfg: Config,
    res: Res
  ): ActorSystem = {
    val system = ActorSystem(name, cfg, cfg.getClass.getClassLoader)
    res.register(new AutoCloseable {
      override def close() = system.terminate()
    })
    system
  }

  @Provides
  def exCont(asys: ActorSystem): ExecutionContext = asys.dispatcher

  @Provides
  @Singleton
  def amat(asys: ActorSystem): ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(asys), "strm")(asys)
}