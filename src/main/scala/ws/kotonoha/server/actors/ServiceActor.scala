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

import javax.inject.Inject

import akka.actor.{Actor, ActorInitializationException, ActorLogging, OneForOneStrategy}
import auth.ClientRegistry
import interop.{JumanMessage, JumanRouter}
import lift.{LiftActorService, LiftMessage}
import akka.actor.SupervisorStrategy.{Escalate, Restart}
import com.google.inject.ConfigurationException
import tags.{TagMessage, TagService}
import ws.kotonoha.server.actors.dict.{ExampleActor, ExampleMessage}
import ws.kotonoha.server.actors.examples.AssignExamplesActor
import ws.kotonoha.server.actors.lift.pertab.{NamedCometMessage, PertabCometManager}
import ws.kotonoha.server.ioc.IocActors

/**
 * @author eiennohito
 * @since 07.01.13 
 */
class ServiceActor @Inject() (
  ioc: IocActors
) extends Actor with ActorLogging {

  import concurrent.duration._

  val assignExamples = context.actorOf(ioc.props[AssignExamplesActor], "aex")
  val mongo = context.actorOf(ioc.props[MongoDBActor], "mongo")
  lazy val jumanActor = context.actorOf(ioc.props[JumanRouter], "juman")
  lazy val securityActor = context.actorOf(ioc.props[SecurityActor], "security")
  lazy val luceneActor = context.actorOf(ioc.props[ExampleSearchActor], "lucene")
  lazy val liftActor = context.actorOf(ioc.props[LiftActorService], "lift")
  lazy val lifetime = context.actorOf(ioc.props[LifetimeActor], "lifetime")
  lazy val cometActor = context.actorOf(ioc.props[PertabCometManager], "comet")
  lazy val clientActor = context.actorOf(ioc.props[ClientRegistry], "clients")
  lazy val tagSvc = context.actorOf(ioc.props[TagService], "tags")
  lazy val globalExampleSvc = context.actorOf(ioc.props[ExampleActor], "examples")

  override def receive = {
    case msg: DbMessage => mongo.forward(msg)
    case msg: JumanMessage => jumanActor.forward(msg)
    case msg: SecurityMessage => securityActor.forward(msg)
    case msg: SearchMessage => luceneActor.forward(msg)
    case msg: LiftMessage => liftActor.forward(msg)
    case msg: LifetimeMessage => lifetime.forward(msg)
    case msg: NamedCometMessage => cometActor.forward(msg)
    case msg: ClientMessage => clientActor.forward(msg)
    case msg: TagMessage => tagSvc.forward(msg)
    case msg: ExampleMessage => globalExampleSvc.forward(msg)
    case msg => log.warning("invalid message came to service actor root {}", msg)
  }

  override def supervisorStrategy() = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 hour) {
    case e: ActorInitializationException if e.getCause.isInstanceOf[ConfigurationException] => Escalate
    case e => log.error(e, "Error in root service actor"); Restart
  }
}
