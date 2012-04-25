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

package org.eiennohito.kotonoha.actors

import akka.pattern.{ask, pipe}
import akka.actor._
import auth.ClientRegistry
import learning._
import model.{CardMessage, WordMessage, CardActor, WordActor}
import org.eiennohito.kotonoha.learning.EventProcessor

/**
 * @author eiennohito
 * @since 25.04.12
 */

trait KotonohaMessage
trait DbMessage extends KotonohaMessage
trait LifetimeMessage extends KotonohaMessage
trait ClientMessage extends KotonohaMessage
trait TokenMessage extends KotonohaMessage

class RestartActor extends Actor with ActorLogging {
  import SupervisorStrategy._
  import akka.util.duration._
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 1500, withinTimeRange = 1 day) {
    case e: Exception => log.error(e, "Caught an exception in root actor"); Restart
  }

  val mongo = context.actorOf(Props[MongoDBActor], "mongo")
  lazy val wordSelector = context.actorOf(Props[WordSelector])
  lazy val markProcessor = context.actorOf(Props[EventProcessor])
  lazy val lifetime = context.actorOf(Props[LifetimeActor])
  lazy val qractor = context.actorOf(Props[QrCreator])
  lazy val clientActor = context.actorOf(Props[ClientRegistry])
  lazy val userToken = context.actorOf(Props[UserTokenActor])
  lazy val luceneActor = context.actorOf(Props[ExampleSearchActor])
  lazy val wordActor = context.actorOf(Props[WordActor])
  lazy val cardActor = context.actorOf(Props[CardActor])


  def dispatch(msg: KotonohaMessage) {
    msg match {
      case m: DbMessage => mongo.forward(msg)
      case m: LifetimeMessage => lifetime.forward(msg)
      case m: QrMessage => qractor.forward(msg)
      case m: ClientMessage => clientActor.forward(msg)
      case m: TokenMessage => userToken.forward(msg)
      case m: SearchMessage => luceneActor.forward(msg)
      case m: WordMessage => wordActor.forward(msg)
      case m: CardMessage => cardActor.forward(msg)
    }
  }

  protected def receive = {
    case TopLevelActors => {
      sender ! (wordSelector, markProcessor)
    }

    case msg : KotonohaMessage => dispatch(msg)
  }
}
