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

import akka.actor.ActorLogging
import akka.util.Timeout
import com.google.inject.Inject
import org.bson.types.ObjectId
import org.joda.time.ReadableDuration
import ws.kotonoha.model.CardMode
import ws.kotonoha.server.actors.{KotonohaMessage, UserScopedActor}
import ws.kotonoha.server.ops.{FlashcardOps, TagCache, WordTagOps}
import ws.kotonoha.server.records.{WordCardRecord, WordRecord}

import scala.concurrent.Future

trait CardMessage extends KotonohaMessage

case class SchedulePaired(wordId: ObjectId, cardType: CardMode) extends CardMessage

case class ChangeCardEnabled(wordId: ObjectId, status: Boolean) extends CardMessage

case class ScheduleLater(card: ObjectId, duration: ReadableDuration) extends CardMessage

case class DeleteCardsForWord(word: ObjectId) extends CardMessage

case object ReprioritizeCards extends CardMessage

case object PrioritiesApplied

case class TagCards(wid: ObjectId, tags: List[String], prio: Int) extends CardMessage

class CardActor @Inject() (
  cops: FlashcardOps,
  tops: WordTagOps
) extends UserScopedActor with ActorLogging {

  import akka.pattern.pipe
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  import concurrent.duration._

  implicit val timeout: Timeout = 1 second

  lazy val mongo = scoped("mongo")
  lazy val word = scoped("word")
  lazy val tags = scoped("tags")

  def reprioritizeCards(c: TagCache): Unit = {
    val q = WordCardRecord where (_.user eqs uid) and (_.learning exists false)
    val res = q select(_.id, _.word) fetch() map {  case (cid, wid) =>
        val pri = WordRecord where (_.id eqs wid) select (_.tags) get() match {
          case None =>
            log.debug("card {} has no word attached (word should have id {})", cid, wid)
            0
          case Some(tgs) =>
            c.priority(tgs)
        }
        cops.setPriority(cid, pri)
    }
    Future.sequence(res).map(_ => PrioritiesApplied) pipeTo sender()
  }

  def tagCards(wid: ObjectId, tags: List[String], prio: Int): Unit = {

  }

  import UserScopedActor._

  override def receive = {
    case SchedulePaired(wid, cardMode) => cops.schedulePaired(wid, cardMode).map(_ => true).pipeTo(sender())
    case ChangeCardEnabled(wid, status) => cops.enableFor(wid, status).map(_ => true).pipeTo(sender())
    case ScheduleLater(card, interval) =>
      cops.scheduleAfter(card, FiniteDuration(interval.getMillis, MILLISECONDS)).logFailure
    case DeleteCardsForWord(wid) => cops.deleteFor(wid).logFailure
    case TagCards(wid, tags, prio) => cops.tagCards(wid, tags, prio).logFailure
    case ReprioritizeCards =>
      tops.calculator pipeTo self
    case c: TagCache =>
      reprioritizeCards(c)
  }
}
