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

package ws.kotonoha.server.ioc

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.google.inject.{Provides, Singleton}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import ws.kotonoha.akane.analyzers.juman._

import scala.concurrent.{Await, ExecutionContext}

/**
  * @author eiennohito
  * @since 2016/08/08
  */
class JumanModule extends ScalaModule {
  override def configure() = {}

  @Provides
  @Singleton
  def jumanActor(
    asys: ActorSystem,
    cfg: Config
  ): JumanActorHolder = {
    val props = Props(new JumanActor(JumanConfig(cfg))).withRouter(RoundRobinPool(nrOfInstances = cfg.getInt("juman.concurrency")))
    val actor = asys.actorOf(props, "juman2")
    JumanActorHolder(actor)
  }

  import scala.concurrent.duration._

  @Provides
  def async(
    jah: JumanActorHolder
  ): AsyncJumanAnalyzer = {
    new JumanActorAnalyzer(jah.ref, 1.second)
  }

  @Provides
  def sync(
    async: AsyncJumanAnalyzer
  )(implicit ec: ExecutionContext): JumanAnalyzer = {
    new JumanAnalyzer {
      override def analyzeSync(input: String) = {
        val future = async.analyze(input)(ec)
        Await.ready(future, 1.second)
        future.value.get
      }
    }
  }
}

case class JumanActorHolder(ref: ActorRef)
