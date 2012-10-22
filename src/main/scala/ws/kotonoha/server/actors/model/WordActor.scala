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
import ws.kotonoha.server.actors.{KotonohaMessage, SaveRecord, RootActor}
import akka.dispatch.{ExecutionContext, Future}
import ws.kotonoha.server.records.{MarkEventRecord, WordCardRecord, WordStatus, WordRecord}
import net.liftweb.common.Empty
import ws.kotonoha.server.learning.ProcessMarkEvents
import net.liftweb.json.JsonAST.JObject
import ws.kotonoha.akane.unicode.KanaUtil

trait WordMessage extends KotonohaMessage
case class RegisterWord(word: WordRecord, state: WordStatus.Value = WordStatus.Approved) extends WordMessage
case class ChangeWordStatus(word: Long, status: WordStatus.Value) extends WordMessage
case class MarkAllWordCards(word: Long, mark: Int) extends WordMessage
case class MarkForDeletion(word: Long) extends WordMessage
case object DeleteReadyWords

class WordActor extends Actor with ActorLogging with RootActor {
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

  protected def receive = {
    case RegisterWord(word, st) => {
      val userId = word.user.is
      val wordid = word.id.is
      var fl = List(ask(root, SaveRecord(word)))
      if (checkReadingWriting(word)) {
        fl ::= root ? RegisterCard(wordid, userId, CardMode.READING)
      }
      fl ::= root ? RegisterCard(wordid, userId, CardMode.WRITING)

      val toReply = sender
      val s = Future.sequence(fl.map(_.mapTo[Boolean])).flatMap(_ => self ? ChangeWordStatus(wordid, st))
      s foreach {
        list =>
          toReply ! word.id.is
      }
    }
    case ChangeWordStatus(word, stat) => {
      import ws.kotonoha.server.util.KBsonDSL._
      val sq: JObject = "_id" -> word
      val uq: JObject = "$set" -> ("status" -> stat.id)
      WordRecord.update(sq, uq)
      val f = stat match {
        case WordStatus.Approved => root ? ChangeCardEnabled(word, true)
        case _ => root ? ChangeCardEnabled(word, false)
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
      (root ? ProcessMarkEvents(data)) andThen {case _ =>
        data map { d => root ! ClearNotBefore(d.card.is) }
      }
    }
    case MarkForDeletion(word) => {
      WordRecord where (_.id eqs word) modify (_.deleteOn setTo(now.plusDays(1))) updateOne()
      self ! ChangeWordStatus(word, WordStatus.Deleting)
    }
    case DeleteReadyWords => {
      val time = now
      val q = WordRecord where (_.deleteOn before time) and (_.status eqs WordStatus.Deleting)
      q foreach { w => { root ! DeleteCardsForWord(w.id.is) } }
      q bulkDelete_!!()
      context.system.scheduler.scheduleOnce(3 hours, self, DeleteReadyWords)
    }
  }

  override def preStart() {
    context.system.scheduler.scheduleOnce(1 hour, self, DeleteReadyWords)
  }
}
