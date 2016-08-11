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

import akka.actor._
import com.google.inject.Inject
import ws.kotonoha.server.actors.dict.{DictionaryActor, ExampleActor, ExampleMessage}
import ws.kotonoha.server.actors.learning._
import ws.kotonoha.server.actors.model.{CardActor, CardMessage, WordActor, WordMessage}
import ws.kotonoha.server.actors.recommend.{RecommendActor, RecommenderMessage}
import ws.kotonoha.server.actors.tags.{TagActor, TagMessage}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.learning.{EventMessage, EventProcessor}

/**
 * @author eiennohito
 * @since 25.04.12
 */

object UserGuardNames {
  val word = "word"
  val card = "card"
  val tags = "tags"
}

class UserGuardActor @Inject() (ioc: UserContext) extends UserScopedActor with ActorLogging {

  import SupervisorStrategy._

  import concurrent.duration._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1.minute) {
    case e: Exception => log.error(e, "Caught an exception in guard actor"); Restart
  }

  import UserGuardNames._

  val mongo = context.actorOf(ioc.props[MongoDBActor], "mongo")
  val wordSelector = context.actorOf(ioc.props[SelectorFacade], "selector")
  val markProcessor = context.actorOf(ioc.props[EventProcessor], "markProc")
  val qractor = context.actorOf(ioc.props[QrCreator], "qr")
  val userToken = context.actorOf(ioc.props[UserTokenActor], "token")
  val wordActor = context.actorOf(ioc.props[WordActor], word)
  val cardActor = context.actorOf(ioc.props[CardActor], card)
  val dictActor = context.actorOf(ioc.props[DictionaryActor], "dict")
  val exampleActor = context.actorOf(ioc.props[ExampleActor], "example")
  val tagActor = context.actorOf(ioc.props[TagActor], tags)
  val recommender = context.actorOf(ioc.props[RecommendActor], "recommend")

  def dispatch(msg: KotonohaMessage) {
    users ! PingUser(uid)
    msg match {
      case _: SelectWordsMessage => wordSelector.forward(msg)
      case _: EventMessage => markProcessor.forward(msg)
      case _: DbMessage => mongo.forward(msg)
      case _: QrMessage => qractor.forward(msg)
      case _: TokenMessage => userToken.forward(msg)
      case _: WordMessage => wordActor.forward(msg)
      case _: CardMessage => cardActor.forward(msg)
      case _: DictionaryMessage => dictActor.forward(msg)
      case _: ExampleMessage => exampleActor.forward(msg)
      case _: TagMessage => tagActor.forward(msg)
      case _: RecommenderMessage => recommender.forward(msg)
    }
  }

  override def receive = {
    case CreateActor(p, name) => name match {
      case null | "" => sender ! context.actorOf(ioc.props(Manifest.classType(p)))
      case _ => sender ! context.actorOf(ioc.props(Manifest.classType(p)), name)
    }
    case msg: KotonohaMessage => dispatch(msg)
  }
}
