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




import akka.util.Timeout
import akka.actor.{ActorLogging, Actor}
import ws.kotonoha.server.model.CardMode
import ws.kotonoha.server.actors.{UserScopedActor, KotonohaMessage, SaveRecord, RootActor}
import akka.dispatch.{ExecutionContext, Future}
import ws.kotonoha.server.records.{MarkEventRecord, WordCardRecord, WordStatus, WordRecord}
import net.liftweb.common.Empty
import ws.kotonoha.server.learning.ProcessMarkEvents
import net.liftweb.json.JsonAST.JObject
import ws.kotonoha.akane.unicode.KanaUtil
import org.bson.types.ObjectId

trait WordMessage extends KotonohaMessage
case class RegisterWord(word: WordRecord, state: WordStatus.Value = WordStatus.Approved) extends WordMessage
case class ChangeWordStatus(word: ObjectId, status: WordStatus.Value) extends WordMessage
case class MarkAllWordCards(word: ObjectId, mark: Int) extends WordMessage
case class MarkForDeletion(word: ObjectId) extends WordMessage
case object DeleteReadyWords

class WordActor extends UserScopedActor with ActorLogging {
  import akka.util.duration._
  import akka.pattern.ask
  import com.foursquare.rogue.Rogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  implicit val timeout: Timeout = 1 second
  implicit val dispatcher = context.dispatcher

  def checkReadingWriting(word: WordRecord) = {
    val read = word.reading.stris
    val writ = word.writing.stris
    if (writ == null || writ == "") {
      word.writing(word.reading.is)
      false
    } else {
      //normalized katakana == hiragana equals
      !KanaUtil.kataToHira(read).equalsIgnoreCase(KanaUtil.kataToHira(writ))
    }
  }

  val mongo = context.actorFor(guardActorPath / "mongo")
  val card = context.actorFor(guardActorPath / "card")

  protected def receive = {
    case RegisterWord(word, st) => {
      val wordid = word.id.is
      var fl = List(ask(mongo, SaveRecord(word)))
      if (checkReadingWriting(word)) {
        fl ::= card ? RegisterCard(wordid, CardMode.READING)
      }
      fl ::= card ? RegisterCard(wordid, CardMode.WRITING)

      val toReply = sender
      val s = Future.sequence(fl.map(_.mapTo[Boolean])).flatMap(_ => self ? ChangeWordStatus(wordid, st))
      s foreach {
        _ => toReply ! word.id.is
      }
    }
    case ChangeWordStatus(word, stat) => {
      import ws.kotonoha.server.util.KBsonDSL._
      val sq: JObject = "_id" -> word
      val uq: JObject = "$set" -> ("status" -> stat.id)
      WordRecord.update(sq, uq)
      val f = stat match {
        case WordStatus.Approved => card ? ChangeCardEnabled(word, true)
        case _ => card ? ChangeCardEnabled(word, false)
      }

      val s = sender
      f map {_ => s ! 1 }
    }
    case MarkAllWordCards(word, mark) => {
      val cards = WordCardRecord where(_.word eqs word) fetch()
      val data = cards map {c => {
        val r =  MarkEventRecord.createRecord
        r.card(c.id.valueBox)
        r.mode(c.cardMode.valueBox)
        r.time(Empty)
        r.mark(mark)
        r.datetime(now)
        r.user(c.user.valueBox)
        r
      }}
      (userActor ? ProcessMarkEvents(data)) andThen {case _ =>
        data map { d => card ! ClearNotBefore(d.card.is) }
      }
    }
    case MarkForDeletion(word) => {
      WordRecord where (_.id eqs word) modify (_.deleteOn setTo(now.plusDays(1))) updateOne()
      self ! ChangeWordStatus(word, WordStatus.Deleting)
    }
    case DeleteReadyWords => {
      val time = now
      val q = WordRecord where (_.deleteOn before time) and (_.status eqs WordStatus.Deleting)
      q foreach { w => { card ! DeleteCardsForWord(w.id.is) } }
      q bulkDelete_!!()
      context.system.scheduler.scheduleOnce(3 hours, self, DeleteReadyWords)
    }
  }

  override def preStart() {
    context.system.scheduler.scheduleOnce(1 hour, self, DeleteReadyWords)
  }
}
