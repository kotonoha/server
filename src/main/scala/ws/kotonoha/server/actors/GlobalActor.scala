/*
 * Copyright 2012 eiennohito
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

package ws.kotonoha.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props}

import concurrent.duration._
import akka.actor.SupervisorStrategy.Restart
import akka.util.Timeout
import com.google.inject.{Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.Await

/**
 * @author eiennohito
 * @since 07.01.13 
 */

class GlobalActor extends Actor with ActorLogging {
  override def supervisorStrategy() = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 hour, loggingEnabled = true) {
    case e => log.error(e, "Error in global actor"); Restart
  }

  val svcs = context.actorOf(Props[ServiceActor], GlobalActor.svcName)
  val user = context.actorOf(Props[UserActorManager], GlobalActor.userName)

  override def receive = {
    case x: MessageForUser => user.forward(x)
    case x => svcs.forward(x)
  }
}

object GlobalActor {
  val globalName = "global"
  val svcName = "services"
  val userName = "users"
}

trait GlobalActors {
  def services: ActorRef
  def users: ActorRef
  def global: ActorRef
}

class GlobalActorsModule extends ScalaModule {
  override def configure() = {}

  @Provides
  @Singleton
  def globalActors(
    asys: ActorSystem
  ): GlobalActors = {
    val gact = asys.actorOf(Props[GlobalActor], GlobalActor.globalName)
    val svcPath = gact.path / GlobalActor.svcName
    val usrPath = gact.path / GlobalActor.userName

    implicit val timeout: Timeout = 5.seconds
    val svcF = asys.actorSelection(svcPath).resolveOne()
    val usrF = asys.actorSelection(usrPath).resolveOne()

    new GlobalActors {
      override val global = gact
      override val users = Await.result(usrF, 5.seconds)
      override val services = Await.result(svcF, 5.seconds)
    }
  }

}
