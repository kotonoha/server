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
import ws.kotonoha.server.records.{WordRecord, UserTagInfo, WordCardRecord}
import ws.kotonoha.server.util.DateTimeUtils
import net.liftweb.common.Empty
import ws.kotonoha.server.actors.{UserScopedActor, SaveRecord, RootActor, KotonohaMessage}
import akka.util.Timeout
import org.joda.time.ReadableDuration
import org.bson.types.ObjectId
import com.mongodb.casbah.WriteConcern
import concurrent.{Future, ExecutionContext}
import ws.kotonoha.server.actors.tags.{CalculatePriority, Priority}

trait CardMessage extends KotonohaMessage

case class SchedulePaired(wordId: ObjectId, cardType: Int) extends CardMessage

case class ChangeCardEnabled(wordId: ObjectId, status: Boolean) extends CardMessage

case class RegisterCard(word: ObjectId, cardMode: Int, priority: Int) extends CardMessage

case class ClearNotBefore(card: ObjectId) extends CardMessage

case class ScheduleLater(card: ObjectId, duration: ReadableDuration) extends CardMessage

case class DeleteCardsForWord(word: ObjectId) extends CardMessage

case object ReprioritizeCards extends CardMessage

case object PrioritiesApplied

case class TagCards(wid: ObjectId, tags: List[String], prio: Int) extends CardMessage

class CardActor extends UserScopedActor with ActorLogging {

  import concurrent.duration._
  import akka.pattern.{ask, pipe}
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import DateTimeUtils._

  implicit val timeout: Timeout = 1 second

  lazy val mongo = scoped("mongo")
  lazy val word = scoped("word")
  lazy val tags = scoped("tags")

  def reprioritizeCards(): Unit = {
    val q = WordCardRecord where (_.user eqs uid) and (_.learning exists false)
    val zero = Priority(0)
    val res = q select(_.id, _.word) fetch() map {
      case (cid, wid) =>
        val pri = WordRecord where (_.id eqs wid) select (_.tags) get() match {
          case None =>
            log.debug("card {} has no word attached (word should have id {})", cid, wid)
            Future.successful(zero)
          case Some(w) => {
            (tags ? CalculatePriority(w)).mapTo[Priority]
          }
        }
        pri.map {
          p => WordCardRecord where (_.id eqs cid) modify (_.priority setTo p.prio) updateOne()
          Nil
        }
    }
    Future.sequence(res).map(_ => PrioritiesApplied) pipeTo sender
  }

  def tagCards(wid: ObjectId, tags: List[String], prio: Int): Unit = {
    val q = WordCardRecord where (_.word eqs wid) modify (_.tags setTo (tags)) and (_.priority setTo (prio))
    q.updateMulti(WriteConcern.Normal)
  }

  override def receive = {
    case SchedulePaired(word, cardMode) => schedulePaired(cardMode, word)
    case ChangeCardEnabled(word, status) => {
      val q = WordCardRecord where (_.word eqs word) modify (_.enabled setTo status)
      q.updateMulti()
      sender ! true
    }
    case RegisterCard(word, mode, pri) => {
      val card = WordCardRecord.createRecord
      val time = now.minusMinutes(5)
      card.user(uid).word(word).cardMode(mode).learning(Empty).notBefore(time).priority(pri)
      val s = sender
      ask(mongo, SaveRecord(card)) pipeTo (s)
    }
    case ClearNotBefore(card) => {
      log.debug("Clearning not before for card id {}", card)
      val q = WordCardRecord where (_.id eqs card) modify (_.notBefore setTo now.minusSeconds(5))
      q updateOne()
    }
    case ScheduleLater(card, interval) => {
      val time = now plus (interval)
      val q = WordCardRecord where (_.id eqs card) modify (_.notBefore setTo (time))
      q.updateOne()
    }
    case DeleteCardsForWord(word) => {
      WordCardRecord where (_.word eqs word) bulkDelete_!! (WriteConcern.Normal)
    }
    case ReprioritizeCards => reprioritizeCards()
    case TagCards(wid, tags, prio) => tagCards(wid, tags, prio)
  }

  def schedulePaired(cardMode: Int, word: ObjectId) {
    val cardType = if (cardMode == CardMode.READING) CardMode.WRITING else CardMode.READING
    val date = now plus ((3.2 * MathUtil.ofrandom) days)
    val q = WordCardRecord where (_.cardMode eqs cardType) and
      (_.word eqs word) modify (_.notBefore setTo date)
    q.updateOne()
    sender ! true
  }
}
