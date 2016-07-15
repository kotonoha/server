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
import akka.actor.ActorLogging
import ws.kotonoha.server.model.CardMode
import ws.kotonoha.server.actors._
import tags.{Priority, CalculatePriority}
import ws.kotonoha.server.records.{WordCardRecord, WordStatus, WordRecord}
import net.liftweb.common.Empty
import ws.kotonoha.akane.unicode.KanaUtil
import org.bson.types.ObjectId
import concurrent.Future
import com.mongodb.casbah.WriteConcern
import ws.kotonoha.server.records.events.MarkEventRecord
import net.liftweb.json.JsonAST.JObject
import ws.kotonoha.server.learning.ProcessMarkEvents
import ws.kotonoha.server.actors.SaveRecord
import ws.kotonoha.server.web.comet.Candidate

trait WordMessage extends KotonohaMessage

case class RegisterWord(word: WordRecord, state: WordStatus.Value = WordStatus.Approved) extends WordMessage
case class ChangeWordStatus(word: ObjectId, status: WordStatus.Value) extends WordMessage
case class MarkAllWordCards(word: ObjectId, mark: Int) extends WordMessage
case class MarkForDeletion(word: ObjectId) extends WordMessage
case object DeleteReadyWords extends WordMessage
case class SimilarWordsRequest(cand: Candidate) extends WordMessage

case class PresentStatus(cand: Candidate, present: List[SimilarWord], queue: List[SimilarWord]) {
  def fullMatch = {
    present.exists(w => w.writings.contains(cand.writing)) || queue.exists(w => w.writings.contains(cand.writing))
  }
}


class WordActor extends UserScopedActor with ActorLogging {

  import concurrent.duration._
  import akka.pattern.{ask, pipe}
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  implicit val timeout: Timeout = 1 second
  implicit val dispatcher = context.dispatcher

  def findSimilar(cand: Candidate): Unit = {
    val ws = SimilarWords.similarWords(uid, cand)
    val adds = SimilarWords.similarAdds(uid, cand)
    sender ! PresentStatus(cand, ws, adds)
  }

  override def receive = {
    case RegisterWord(word, st) => registerWord(word, st)
    case ChangeWordStatus(word, stat) => changeWordStatus(word, stat)
    case MarkAllWordCards(word, mark) => markAllCards(word, mark)
    case DeleteReadyWords => deleteReadyWords()
    case MarkForDeletion(word) =>
      WordRecord where (_.id eqs word) modify (_.deleteOn setTo (now.plusDays(1))) updateOne()
      self ! ChangeWordStatus(word, WordStatus.Deleting)
    case SimilarWordsRequest(c) => findSimilar(c)
  }

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
          d => card ! ClearNotBefore(d.card.is)
        }
    }

  }

  def changeWordStatus(word: ObjectId, stat: WordStatus.Value) {

    import ws.kotonoha.server.util.KBsonDSL._
    val sq: JObject = "_id" -> word
    val uq: JObject = "$set" -> ("status" -> stat.id)
    WordRecord.update(sq, uq)
    val f = stat match {
      case WordStatus.Approved => card ? ChangeCardEnabled(word, true)
      case _ => card ? ChangeCardEnabled(word, false)
    }
    f map {
      x => 1
    } pipeTo sender

  }

  def registerWord(word: WordRecord, st: WordStatus.Value) {
    val wordid = word.id.get
    val priF = (tags ? CalculatePriority(word.tags.get)).mapTo[Priority]
    var futures = List(ask(mongo, SaveRecord(word)))
    if (checkReadingWriting(word)) {
      futures ::= priF flatMap {
        p => card ? RegisterCard(wordid, CardMode.READING, p.prio)
      }
    }
    futures ::= priF flatMap {
      p => card ? RegisterCard(wordid, CardMode.WRITING, p.prio)
    }

    val s = Future.sequence(futures.map(_.mapTo[Boolean])).flatMap(_ => self ? ChangeWordStatus(wordid, st))
    s map {
      x => wordid
    } pipeTo sender

  }

  def deleteReadyWords(): Unit = {
    val time = now
    val q = WordRecord where (_.deleteOn lt time) and (_.status eqs WordStatus.Deleting)
    q foreach {
      w => {
        card ! DeleteCardsForWord(w.id.get)
      }
    }
    q.bulkDelete_!!(WriteConcern.Normal)
    context.system.scheduler.scheduleOnce(1.hour, users, ForUser(uid, DeleteReadyWords))
  }

  override def preStart() {
    context.system.scheduler.scheduleOnce(5.minutes, self, DeleteReadyWords)
  }
}
