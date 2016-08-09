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

package ws.kotonoha.server.web.comet

import akka.actor.{ActorRef, Status}
import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import com.google.inject.Inject
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http.js.JE.{Call, JsRaw}
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.{RenderOut, ShutDown}
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST.{JField, JObject, JValue}
import net.liftweb.util.Helpers.TimeSpan
import org.apache.lucene.search.BooleanClause.Occur
import org.bson.types.ObjectId
import ws.kotonoha.akane.dic.jmdict.JmdictTag
import ws.kotonoha.dict.jmdict.{JmdictQuery, JmdictQueryPart, LuceneJmdict}
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import ws.kotonoha.server.actors.learning.{LoadWords, WordsAndCards}
import ws.kotonoha.server.actors.lift.{AkkaInterop, Ping}
import ws.kotonoha.server.actors.schedulers.{RepetitionStateResolver, ReviewCard}
import ws.kotonoha.server.japanese.ConjObj
import ws.kotonoha.server.learning.ProcessMarkEvent
import ws.kotonoha.server.records._
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.util.DateTimeUtils

import scala.util.Random
import scala.xml.{NodeSeq, Text, Utility}

/**
 * @author eiennohito
 * @since 21.05.12
 */

case class RecieveJson(obj: JValue)

case class RepeatUser(id: ObjectId)

case class WebMark(card: String, mode: Int, time: Double, mark: Int, remaining: Int)

case object UpdateNum

class RepeatActor @Inject() (
  jms: LuceneJmdict
) extends NamedCometActor with AkkaInterop with Logging with ReleaseAkka {
  import DateTimeUtils._

  import concurrent.duration._

  def self = this

  var userId: ObjectId = null
  var uact: ActorRef = _
  var count = 0

  lazy val today = new RepetitionStateResolver(userId).today
  implicit def context = akkaServ.context

  val cancellable = akkaServ.system.scheduler.schedule(5 minutes, 1 minute)(self ! Ping)

  var last = now

  def render = {
    val js = JsCmds.Function("send_to_actor", List("obj"), jsonSend(JsRaw("obj")))
    RenderOut(Full(defaultHtml), Empty, Full(jsonToIncludeInCode & js), Empty, ignoreHtmlOnJs = true)
  }


  override def localShutdown {
    cancellable.cancel()
    super.localShutdown
  }

  override def receiveJson = {
    case o => self ! RecieveJson(o); JsCmds.Noop
  }

  def processJson(obj: JValue): Unit = {
    implicit val formats = DefaultFormats
    import net.liftweb.json.Extraction.extract
    obj match {
      case JObject(JField("command", data) :: _) => {
        val mark = extract[WebMark](data)
        self ! mark
        self ! UpdateNum
      }
      case _ => logger.debug("invalid json: " + obj)
    }
  }


  override protected def dontCacheRendering = true

  def nsString(in: NodeSeq) = {
    val sb = new StringBuilder
    Utility.sequenceToXML(in, sb = sb)
    sb.toString()
  }

  def processWord(writing: String, reading: Option[String]): Option[NodeSeq] = try {
    val q = JmdictQuery(
      limit = 5,
      writings = Seq(JmdictQueryPart(writing, Occur.MUST)),
      readings = reading.map(r => JmdictQueryPart(r, Occur.MUST)).toSeq
    )
    val entries = jms.find(q)
    val meanings = entries.data.headOption.toSeq.flatMap(_.meanings)
    val word_type = meanings.flatMap(_.pos)
    val cobj = ConjObj(word_type.headOption.getOrElse(JmdictTag.exp).name, writing)
    val ns1 = cobj.masuForm.data map {c => <div>{c}</div> }
    val ns2 = cobj.teForm.data map {c => <div>{c}</div>}
    ns1 flatMap{s => ns2 map {t => s ++ t}}
  } catch { case _: Throwable => None }

  def publish(words: List[WordRecord], cards: List[WordCardRecord], seq: List[ReviewCard]): Unit = {
    import net.liftweb.json.{compactRender}
    import ws.kotonoha.server.util.KBsonDSL._
    val wm = words.map(w => (w.id.get, w)).toMap
    val cm = cards.map(c => (c.id.get, c)).toMap
    def getExamples(in: List[ExampleRecord], max: Int) = {
      val selected = Random.shuffle(in).take(max)
      val html = selected flatMap (e =>
        <div class="nihongo">
          {e.example.get}
        </div>
          <div>
            {e.translation.get}
          </div>)
      nsString(html)
    }
    val data: JValue = seq map (it => {
      val c = cm(it.cid)
      val w = wm(c.word.get)
      val addInfo = processWord(w.writing.stris, Some(w.reading.stris))
      val procInfo = addInfo map {
        nsString(_)
      } getOrElse ""
      ("writing" -> w.writing.stris) ~ ("reading" -> w.reading.stris) ~ ("meaning" -> w.meaning) ~
        ("cid" -> c.id.get.toString) ~ ("mode" -> c.cardMode.get) ~ ("examples" -> getExamples(w.examples.get, 5)) ~
        ("additional" -> procInfo) ~ ("wid" -> c.word.get.toString) ~ ("src" -> it.source)
    })
    partialUpdate(Call("publish_new", compactRender(data)).cmd)
  }

  def processMark(mark: WebMark): Unit = {
    import DateTimeUtils._

    import concurrent.duration._

    val me = MarkEventRecord.createRecord
    val cid = new ObjectId(mark.card)
    me.card(cid).mark(mark.mark).mode(mark.mode).time(mark.time)
    me.client("web-repeat")
    me.user(userId)
    me.datetime(now)
    val f = akka.pattern.ask(uact, ProcessMarkEvent(me))(10 seconds)
    f.onSuccess {
      case x: Int =>
        if (mark.remaining < 5) {
          uact ! LoadWords(15, mark.remaining)
        }
    }
  }

  override def lowPriority = {
    case RepeatUser(id) => {
      userId = id
      uact = akkaServ.userActor(userId)
      uact ! LoadWords(15, 0)
    }
    case RecieveJson(o) => processJson(o)
    case WordsAndCards(words, cards, seq) => publish(words, cards, seq)
    case m: WebMark =>
      last = now
      processMark(m)
    case UpdateNum => {
      partialUpdate(SetHtml("rpt-num", Text(s"Repeated $count word(s) in session, ${today + count} today")))
      count += 1
    }
    case _: Int => //do nothing
    case Status.Failure(ex) => logger.error("error in akka", ex)
    case Ping =>
      val dur = new org.joda.time.Duration(last, now)
      if (dur.getStandardMinutes > 13) {
        partialUpdate(Call("learning_timeout").cmd)
        self ! ShutDown
      }
  }

  override def lifespan: Box[TimeSpan] = Full(15 minutes)
}
