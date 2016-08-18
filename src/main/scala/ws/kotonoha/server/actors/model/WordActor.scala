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
import com.mongodb.casbah.WriteConcern
import net.liftweb.common.Empty
import net.liftweb.json.JsonAST.JObject
import org.bson.types.ObjectId
import ws.kotonoha.model.WordStatus
import ws.kotonoha.server.actors._
import ws.kotonoha.server.learning.ProcessMarkEvents
import ws.kotonoha.server.ops.{SimilarWordOps, WordOps}
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.records.{WordCardRecord, WordRecord}

trait WordMessage extends KotonohaMessage

case class RegisterWord(word: WordRecord, state: WordStatus = WordStatus.Approved) extends WordMessage
case class ChangeWordStatus(word: ObjectId, status: WordStatus) extends WordMessage
case class MarkAllWordCards(word: ObjectId, mark: Int) extends WordMessage
case class MarkForDeletion(word: ObjectId) extends WordMessage
case object DeleteReadyWords extends WordMessage
case class SimilarWordsRequest(cand: Candidate) extends WordMessage

class WordActor @Inject() (
  wops: WordOps,
  swops: SimilarWordOps
) extends UserScopedActor with ActorLogging {

  import akka.pattern.{ask, pipe}
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  import concurrent.duration._

  implicit val timeout: Timeout = 1 second
  implicit val dispatcher = context.dispatcher


  override def receive = {
    case RegisterWord(word, st) => wops.register(word, st).map(_ => word.id.get).pipeTo(sender())
    case ChangeWordStatus(word, stat) => changeWordStatus(word, stat)
    case MarkAllWordCards(word, mark) => markAllCards(word, mark)
    case DeleteReadyWords => deleteReadyWords()
    case MarkForDeletion(word) =>
      WordRecord where (_.id eqs word) modify (_.deleteOn setTo (now.plusDays(1))) updateOne()
      self ! ChangeWordStatus(word, WordStatus.Deleting)
    case SimilarWordsRequest(c) => swops.similarRegistered(c).pipeTo(sender())
  }

  import ActorUtil.aOf

  lazy val mongo = context.actFor(guardActorPath / "mongo")
  lazy val card = context.actFor(guardActorPath / "card")
  lazy val tags = scoped("tags")

  def markAllCards(word: ObjectId, mark: Int) {

    val cards = WordCardRecord where (_.word eqs word) fetch()
    val data = cards map {
      c => {
        val r = MarkEventRecord.createRecord
        r.card(c.id.valueBox)
        r.mode(c.cardMode.valueBox)
        r.time(Empty)
        r.mark(mark)
        r.datetime(now)
        r.user(c.user.valueBox)
        r
      }
    }
    (userActor ? ProcessMarkEvents(data)) andThen {
      case _ =>
        data map {
          d => card ! ClearNotBefore(d.card.get)
        }
    }

  }

  def changeWordStatus(word: ObjectId, stat: WordStatus) {

    import ws.kotonoha.server.util.KBsonDSL._
    val sq: JObject = "_id" -> word
    val uq: JObject = "$set" -> ("status" -> stat.value)
    WordRecord.update(sq, uq)
    val f = stat match {
      case WordStatus.Approved => card ? ChangeCardEnabled(word, true)
      case _ => card ? ChangeCardEnabled(word, false)
    }
    f map {
      x => 1
    } pipeTo sender

  }

  def deleteReadyWords(): Unit = {
    val time = now
    val q = WordRecord where (_.user eqs uid) and (_.deleteOn lt time) and (_.status eqs WordStatus.Deleting)
    q foreach {
      w => {
        card ! DeleteCardsForWord(w.id.get)
      }
    }
    q.bulkDelete_!!(WriteConcern.Normal)

    val cnt = WordRecord where (_.user eqs uid) and (_.status eqs WordStatus.Deleting) count()
    if (cnt > 0) {
      context.system.scheduler.scheduleOnce(1.day, users, ForUser(uid, DeleteReadyWords))
    }
  }

  override def preStart() {
    context.system.scheduler.scheduleOnce(5.minutes, self, DeleteReadyWords)
  }
}
