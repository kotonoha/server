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

package ws.kotonoha.server.actors.model

import akka.actor.{ActorLogging, Actor}
import ws.kotonoha.server.model.CardMode
import ws.kotonoha.server.math.MathUtil
import ws.kotonoha.server.records.WordCardRecord
import ws.kotonoha.server.util.DateTimeUtils
import net.liftweb.common.Empty
import ws.kotonoha.server.actors.{SaveRecord, RootActor, KotonohaMessage}
import akka.util.Timeout
import org.joda.time.ReadableDuration

trait CardMessage extends KotonohaMessage
case class SchedulePaired(wordId: Long, cardType: Int) extends CardMessage
case class ChangeCardEnabled(wordId: Long, status: Boolean) extends CardMessage
case class RegisterCard(word: Long, userId: Long, cardMode: Int) extends CardMessage
case class ClearNotBefore(card: Long) extends CardMessage
case class ScheduleLater(card: Long, duration: ReadableDuration) extends CardMessage
case class DeleteCardsForWord(word: Long)

class CardActor extends Actor with ActorLogging with RootActor {
  import akka.util.duration._
  import akka.pattern.{ask, pipe}
  import com.foursquare.rogue.Rogue._
  import DateTimeUtils._

  implicit val timeout: Timeout = 1 second

  protected def receive = {
    case SchedulePaired(word, cardMode) => {
      val cardType = if (cardMode == CardMode.READING) CardMode.WRITING else CardMode.READING
      val date = now plus ((3.2 * MathUtil.ofrandom) days)
      val q = WordCardRecord where (_.cardMode eqs cardType) and
        (_.word eqs word) modify (_.notBefore setTo date)
      q.updateOne()
      sender ! true
    }
    case ChangeCardEnabled(word, status) => {
      val q = WordCardRecord where (_.word eqs word) modify (_.enabled setTo status)
      q.updateMulti()
      sender ! true
    }
    case RegisterCard(word, user, mode) => {
      val card = WordCardRecord.createRecord
      card.user(user).word(word).cardMode(mode).learning(Empty).notBefore(now)
      val s = sender
      ask(root, SaveRecord(card)) pipeTo (s)
    }
    case ClearNotBefore(card) => {
      log.debug("Clearning not before for card id {}", card)
      WordCardRecord where (_.id eqs card) modify(_.notBefore setTo(now))
    }
    case ScheduleLater(card, interval) => {
      val time = now plus (interval)
      WordCardRecord where(_.id eqs card) modify (_.notBefore setTo(time))
    }
    case DeleteCardsForWord(word) => {
      WordCardRecord where (_.word eqs word) bulkDelete_!!()
    }
  }
}
