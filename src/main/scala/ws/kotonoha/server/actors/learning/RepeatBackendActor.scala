/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.actors.learning

import akka.actor.{Actor, ActorLogging}
import com.google.inject.Inject
import net.liftweb.http.CometActor
import org.apache.lucene.search.BooleanClause.Occur
import org.bson.types.ObjectId
import org.joda.time.DateTime
import ws.kotonoha.akane.dic.jmdict.JmdictTag
import ws.kotonoha.akane.dic.lucene.jmdict.LuceneJmdict
import ws.kotonoha.akane.dic.lucene.jmdict.{JmdictQuery, JmdictQueryPart}
import ws.kotonoha.model.CardMode
import ws.kotonoha.server.actors.learning.RepeatBackend.{MarkAddition, RepCount, WebMark}
import ws.kotonoha.server.actors.learning.RepeatBackendActor.{MarkInProcessing, UpdateOne}
import ws.kotonoha.server.actors.schedulers.RepetitionStateResolver
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.japanese.ConjObj
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.records.{ExampleRecord, WordRecord}
import ws.kotonoha.server.util.DateTimeUtils

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Failure, Random, Success}


object RepeatBackend {
  case class RepQuestionPart(content: String, ruby: Option[String], target: Boolean)
  case class ReviewExample(id: Long, sentence: String, translation: String)
  case class RepAdditional(title: String, value: String)
  case class RepCard(id: String, word: String, mode: CardMode, writings: String, readings: String,
    question: Seq[RepQuestionPart], exs: Seq[ReviewExample], meaning: String, addinfo: Seq[RepAdditional], rexIdx: Int)

  case class WebMark(card: String, mark: Int, remaining: Int, timestamp: Long, questionTime: Long, answerTime: Long, source: String, exId: Int)
  case class MarkAddition(card: String, readyTime: Long)

  case class RepCount(curSession: Int, today: Int)
}

class RepeatBackend @Inject() (
  jms: LuceneJmdict
) {
  import RepeatBackend._

  def makeCards(wnc: WordsAndCards): Seq[RepCard] = {
    val wm = wnc.words.map(w => (w.id.get, w)).toMap
    val cm = wnc.cards.map(c => (c.id.get, c)).toMap
    val data2 = wnc.sequence.map { s =>
      val cid = s.cid
      val card = cm(cid)
      val word = wm(card.word.get)
      val (sid, question) = CardRepetitionQuestion.formatQuestion(word, card.cardMode.value)
      RepCard(
        id = cid.toHexString,
        word = word.id.get.toHexString,
        mode = card.cardMode.get,
        writings = word.writing.get.mkString(", "),
        readings = word.reading.get.mkString(", "),
        meaning = word.meaning.value,
        question = question,
        exs = getExamples(word.examples.get, 5),
        addinfo = verbConjugations(word),
        rexIdx = sid
      )
    }
    data2
  }

  private def getExamples(in: List[ExampleRecord], max: Int) = {
    val selected = Random.shuffle(in).take(max)
    selected.map { er =>
      ReviewExample(
        er.id.get,
        er.example.get,
        er.translation.get
      )
    }
  }

  private def verbConjugations(w: WordRecord): Seq[RepAdditional] = try {
    val wrs = w.writing.get

    val q = JmdictQuery(
      limit = 5,
      writings = wrs.map(w => JmdictQueryPart(w, Occur.MUST)),
      readings = w.reading.get.map(r => JmdictQueryPart(r, Occur.MUST))
    )
    val entries = jms.find(q)
    val meanings = entries.data.headOption.toSeq.flatMap(_.meanings)
    val word_type = meanings.flatMap(_.pos)
    val res = new ArrayBuffer[RepAdditional]()

    for {
      w <- wrs.headOption
      cobj = ConjObj(word_type.headOption.getOrElse(JmdictTag.exp).name, w)
    } {
      cobj.masuForm.data.foreach(f => res += RepAdditional("polite form", f))
      cobj.teForm.data.foreach(f => res += RepAdditional("-te form", f))
    }

    res
  } catch { case _: Throwable =>  Nil }
}

class RepeatBackendActor @Inject() (
  backend: RepeatBackend,
  uc: UserContext,
  meo: MarkEventOps
) extends Actor with ActorLogging {
  import ws.kotonoha.server.web.comet.RepeatActor._

  private var frontend: CometActor = null
  private var inSession = 0
  private lazy val today = new RepetitionStateResolver(uc.uid).today.toInt

  private val marksForCard = new mutable.HashMap[ObjectId, MarkInProcessing]()

  private def handleMark(mark: WebMark): Unit = {
    import DateTimeUtils._

    val repStart = atMillis(mark.questionTime)
    val answerShown = atMillis(mark.answerTime)
    val markTime = atMillis(mark.timestamp)
    val questionDur = duration(repStart, answerShown)
    val answerDur = duration(answerShown, markTime)


    val me = MarkEventRecord.createRecord
    val cid = new ObjectId(mark.card)

    val timeSecs = (questionDur.getMillis + answerDur.getMillis) / 1000.0
    me.card(cid).mark(mark.mark).datetime(markTime).time(timeSecs)
    me.client(s"web-repeat-${mark.source}")
    me.user(uc.uid)
    me.answerDur(answerDur.getMillis / 1000.0)
    me.questionDur(questionDur.getMillis / 1000.0)
    me.wordExIdx(mark.exId)

    val f = meo.process(me)
    marksForCard.put(cid, MarkInProcessing(f, now))

    f.onComplete {
      case Success(_) =>
        if (mark.remaining < 5) {
          self ! LoadWords(15, mark.remaining)
        }
        self ! UpdateOne
      case Failure(t) =>
        log.error(t, "could not process mark: {}", mark)
    }(context.dispatcher)
  }

  def handleAddition(m: MarkAddition): Unit = {
    val cid = new ObjectId(m.card)
    marksForCard.remove(cid) match {
      case None => //do nothing
      case Some(mp) =>
        mp.result.foreach { mark =>
          val time = DateTimeUtils.atMillis(m.readyTime)
          val dur = DateTimeUtils.duration(mark.datetime.get, time)
          meo.setReadyTime(mark.id.get, dur.getMillis / 1000.0)
        }(context.dispatcher)
    }
  }

  private var loadingWords = false
  private var noWords = false

  override def receive: Receive = {
    case a: CometActor =>
      frontend = a
      self ! LoadWords(15, 0)
      signalNumber()
    case lw: LoadWords =>
      if (!loadingWords && !noWords) {
        uc.actor ! lw
        loadingWords = true
      }
    case w: WordsAndCards =>
      frontend ! PublishCards(backend.makeCards(w))
      if (w.sequence.isEmpty) {
        log.debug("no more words to repeat for user={}", uc.uid)
        noWords = true
      }
      loadingWords = false
    case m: WebMark => handleMark(m)
    case m: MarkAddition => handleAddition(m)
    case UpdateOne =>
      inSession += 1
      signalNumber()
  }

  private def signalNumber(): Unit = {
    frontend ! WebMsg("rep-cnt", RepCount(inSession, today + inSession))
  }
}

object RepeatBackendActor {
  case class MarkInProcessing(result: Future[MarkEventRecord], date: DateTime)
  case class MarkProcessed(mer: MarkEventRecord, web: WebMark)
  case object UpdateOne
}
