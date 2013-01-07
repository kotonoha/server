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

import akka.actor.{ActorLogging, OneForOneStrategy, Props, Actor}
import akka.util.duration._
import akka.actor.SupervisorStrategy.Restart

/**
 * @author eiennohito
 * @since 07.01.13 
 */

class GlobalActor extends Actor with ActorLogging {
  override def supervisorStrategy() = OneForOneStrategy(maxNrOfRetries = 10, 1 hour) {
    case e => log.error(e, "Error in global actor"); Restart
  }

  val svcs = context.actorOf(Props[ServiceActor], GlobalActor.svcName)
  val user = context.actorOf(Props[UserActorManager], GlobalActor.userName)

  protected def receive = {
    case x: ForUser => user.forward(x)
    case x: UserActor => user.forward(x)
    case x => svcs.forward(x)
  }
}

object GlobalActor {
  val globalName = "global"
  val svcName = "services"
  val userName = "users"
}
