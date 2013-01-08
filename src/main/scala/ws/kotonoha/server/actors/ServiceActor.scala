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
import auth.ClientRegistry
import interop.{JumanMessage, JumanRouter}
import lift.{LiftMessage, LiftActorService}
import com.fmpwizard.cometactor.pertab.namedactor.{NamedCometMessage, PertabCometManager}
import akka.actor.SupervisorStrategy.Restart
import tags.TagService

/**
 * @author eiennohito
 * @since 07.01.13 
 */
class ServiceActor extends Actor with ActorLogging {
  import akka.util.duration._

  val mongo = context.actorOf(Props[MongoDBActor], "mongo")
  lazy val jumanActor = context.actorOf(Props[JumanRouter], "juman")
  lazy val securityActor = context.actorOf(Props[SecurityActor], "security")
  lazy val luceneActor = context.actorOf(Props[ExampleSearchActor], "lucene")
  lazy val liftActor = context.actorOf(Props[LiftActorService], "lift")
  lazy val lifetime = context.actorOf(Props[LifetimeActor], "lifetime")
  lazy val cometActor = context.actorOf(Props[PertabCometManager], "comet")
  lazy val clientActor = context.actorOf(Props[ClientRegistry], "clients")
  lazy val tagSvc = context.actorOf(Props[TagService], "tags")

  protected def receive = {
    case msg: DbMessage => mongo.forward(msg)
    case msg: JumanMessage => jumanActor.forward(msg)
    case msg: SecurityMessage => securityActor.forward(msg)
    case msg: SearchMessage => luceneActor.forward(msg)
    case msg: LiftMessage => liftActor.forward(msg)
    case msg: LifetimeMessage => lifetime.forward(msg)
    case msg: NamedCometMessage => cometActor.forward(msg)
    case msg: ClientMessage => clientActor.forward(msg)
  }

  override def supervisorStrategy() = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 hour) {
    case e => log.error(e, "Error in root service actor"); Restart
  }
}
