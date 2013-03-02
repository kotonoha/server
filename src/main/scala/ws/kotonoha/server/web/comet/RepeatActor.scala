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

import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import ws.kotonoha.server.actors.lift.AkkaInterop
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.RenderOut
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.js.{JE, JsCmds}
import ws.kotonoha.server.actors.learning.{WordsAndCards, LoadWords}
import util.Random
import net.liftweb.http.js.JE.{Call, JsRaw}
import net.liftweb.json.DefaultFormats
import ws.kotonoha.server.records._
import events.MarkEventRecord
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.server.learning.ProcessMarkEvent
import xml.{Text, NodeSeq, Utility}
import net.liftweb.json.JsonAST.{JField, JObject, JValue}
import net.liftweb.http.js.JsCmds.SetHtml
import org.bson.types.ObjectId
import akka.actor.{Status, ActorRef}
import com.typesafe.scalalogging.slf4j.Logging
import ws.kotonoha.server.actors.schedulers.ReviewCard

/**
 * @author eiennohito
 * @since 21.05.12
 */

case class RecieveJson(obj: JValue)

case class RepeatUser(id: ObjectId)

case class WebMark(card: String, mode: Int, time: Double, mark: Int, remaining: Int)

case object UpdateNum

trait RepeatActorT extends NamedCometActor with AkkaInterop with Logging {
  def self = this

  var userId: ObjectId = null
  var uact: ActorRef = _
  var count = 0

  def render = {
    val js = JsCmds.Function("send_to_actor", List("obj"), jsonSend(JsRaw("obj")))
    RenderOut(Full(defaultHtml), Empty, Full(jsonToIncludeInCode & js), Empty, ignoreHtmlOnJs = true)
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

  def publish(words: List[WordRecord], cards: List[WordCardRecord], seq: List[ReviewCard]): Unit = {
    import ws.kotonoha.server.util.KBsonDSL._
    import net.liftweb.json.{compact => jc, render => jr}
    import ws.kotonoha.server.util.WordUtils.processWord
    val wm = words.map(w => (w.id.is, w)).toMap
    val cm = cards.map(c => (c.id.is, c)).toMap
    def getExamples(in: List[ExampleRecord], max: Int) = {
      val selected = Random.shuffle(in).take(max)
      val html = selected flatMap (e =>
        <div class="nihongo">
          {e.example.is}
        </div>
          <div>
            {e.translation.is}
          </div>)
      nsString(html)
    }
    val data: JValue = seq map (it => {
      val c = cm(it.cid)
      val w = wm(c.word.is)
      val addInfo = processWord(w.writing.stris, Some(w.reading.stris))
      val procInfo = addInfo map {
        nsString(_)
      } getOrElse ""
      ("writing" -> w.writing.stris) ~ ("reading" -> w.reading.stris) ~ ("meaning" -> w.meaning) ~
        ("cid" -> c.id.is.toString) ~ ("mode" -> c.cardMode.is) ~ ("examples" -> getExamples(w.examples.is, 5)) ~
        ("additional" -> procInfo) ~ ("wid" -> c.word.is.toString) ~ ("src" -> it.source)
    })
    partialUpdate(Call("publish_new", jc(jr(data))).cmd)
  }

  def processMark(mark: WebMark): Unit = {
    import DateTimeUtils._
    if (mark.remaining < 5) {
      uact ! LoadWords(15, 0)
    }
    val me = MarkEventRecord.createRecord
    val cid = new ObjectId(mark.card)
    me.card(cid).mark(mark.mark).mode(mark.mode).time(mark.time)
    me.user(userId)
    me.datetime(now)
    uact ! ProcessMarkEvent(me)
  }

  override def lowPriority = {
    case RepeatUser(id) => {
      userId = id
      uact = akkaServ.userActor(userId)
      uact ! LoadWords(15, 0)
    }
    case RecieveJson(o) => processJson(o)
    case WordsAndCards(words, cards, seq) => publish(words, cards, seq)
    case m: WebMark => processMark(m)
    case UpdateNum => {
      partialUpdate(SetHtml("rpt-num", Text("Repeated %d word(s)".format(count))))
      count += 1
    }
    case _: Int => //do nothing
    case Status.Failure(ex) => logger.error("error in akka", ex)
  }
}

class RepeatActor extends RepeatActorT with ReleaseAkka
