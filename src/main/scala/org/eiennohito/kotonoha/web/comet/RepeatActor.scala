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

package org.eiennohito.kotonoha.web.comet

import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import org.eiennohito.kotonoha.actors.lift.AkkaInterop
import com.weiglewilczek.slf4s.Logging
import org.eiennohito.kotonoha.actors.ioc.ReleaseAkka
import net.liftweb.http.RenderOut
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.js.{JsCmd, JsCmds}
import org.eiennohito.kotonoha.actors.learning.{WordsAndCards, LoadWords, LoadCards}
import util.Random
import net.liftweb.http.js.JE.{Call, JsRaw}
import net.liftweb.json.DefaultFormats
import org.eiennohito.kotonoha.records._
import org.eiennohito.kotonoha.util.DateTimeUtils
import org.eiennohito.kotonoha.learning.ProcessMarkEvent
import org.eiennohito.kotonoha.util.unapply.XHexLong
import xml.{Node, Utility}
import net.liftweb.json.JsonAST.{JField, JObject, JValue}

/**
 * @author eiennohito
 * @since 21.05.12
 */

case class RecieveJson(obj: JValue)
case class RepeatUser(id: Long)
case class WebMark(card: String, mode: Int, time: Double, mark: Int, remaining: Int)

trait RepeatActorT extends NamedCometActor with AkkaInterop with Logging {
  def self = this
  val root = akkaServ.root
  var userId: Long = -1

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
      }
      case _ => logger.debug("invalid json: " + obj)
    }
  }


  override protected def dontCacheRendering = true

  def publish(words: List[WordRecord], cards: List[WordCardRecord]): Unit = {
    import org.eiennohito.kotonoha.util.KBsonDSL._
    import net.liftweb.json.{compact => jc, render => jr}
    import org.eiennohito.kotonoha.util.WordUtils.processWord
    val wm = words.map(w => (w.id.is, w)).toMap
    def getExamples(in: List[ExampleRecord], max: Int) = {
      val selected = Random.shuffle(in).take(max)
      val html = selected flatMap(e =>
        <div class="nihongo">{e.example.is}</div>
        <div>{e.translation.is}</div>)
      val sb = new StringBuilder
      Utility.sequenceToXML(html, sb = sb)
      sb.toString()
    }
    val data: JValue = cards map (c => {
      val w = wm(c.word.is)
      ("writing" -> w.writing) ~ ("reading" -> w.reading) ~ ("meaning" -> w.meaning) ~
      ("cid" -> c.id.is.toHexString) ~ ("mode" -> c.cardMode.is) ~ ("examples" -> getExamples(w.examples.is, 5)) ~
      ("additional" -> processWord(w.writing.toString()).getOrElse(""))
    })
    partialUpdate(Call("publish_new", jc(jr(data))).cmd)
  }

  def processMark(mark: WebMark): Unit = {
    import DateTimeUtils._
    if (mark.remaining < 5) {
      root ! LoadWords(userId, 15)
    }
    val me = MarkEventRecord.createRecord
    val XHexLong(cid) = mark.card
    me.card(cid).mark(mark.mark).mode(mark.mode).time(mark.time)
    me.user(userId)
    me.datetime(now)
    root ! ProcessMarkEvent(me)
  }

  override def lowPriority = {
    case RepeatUser(id) =>  {
      userId = id
      root ! LoadWords(id, 15)
    }
    case RecieveJson(o) => processJson(o)
    case WordsAndCards(words, cards) => publish(words, cards)
    case m: WebMark => processMark(m)
    case _: Int => //do nothing
  }
}

class RepeatActor extends RepeatActorT with ReleaseAkka
