/*
 * Copyright 2012-2013 eiennohito
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

import net.liftweb.http.{S, CometActor}
import net.liftweb.common.{Box, Full}
import ws.kotonoha.server.actors.lift.NgLiftActor
import ws.kotonoha.server.web.loc.WikiPage
import net.liftweb.json.JsonAST.{JValue, JString, JObject}
import net.liftweb.http.js.JsCmds
import net.liftweb.json.{DefaultFormats, Extraction}
import ws.kotonoha.server.records.{UserRecord, WikiPageRecord}
import org.bson.types.ObjectId
import ws.kotonoha.server.wiki.{WikiLinkCache, WikiRenderer}
import scala.xml.{NodeSeq, Group}
import com.typesafe.scalalogging.slf4j.Logging
import net.liftweb.http.js.JsCmds.RedirectTo
import ws.kotonoha.server.wiki.template.TemplateParams
import com.tristanhunt.knockoff.{SanitizationEvent, SanitizationChangeSupport}
import scala.collection.mutable.ListBuffer

/**
 * @author eiennohito
 * @since 17.04.13 
 */

case class Data(src: String, comment: Option[String])
case class Save(obj: JValue)

class EditWiki extends CometActor with NgLiftActor with Logging {
  import ws.kotonoha.server.util.DateTimeUtils._
  import concurrent.duration._
  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.KBsonDSL._

  def self = this

  override def render = {
    loaded = true
    val xhtml = S.runTemplate("templates-hidden" :: "wiki-edit" :: Nil)
    val ro = super.render
    if (pageInfo != null)
      self ! pageInfo
    ro.copy(xhtml = xhtml)
  }

  def svcName = "WikiEdit"

  var pageInfo: WikiPage = _
  var loaded = false
  var pid: ObjectId = null

  override def lifespan = Full(3 minutes)

  implicit val formats = DefaultFormats

  def update(in: JValue) = {
    val x = Extraction.extract[Data](in)
    val xhtml: NodeSeq = makePage(x)
    val w = new java.io.StringWriter(65536) //64k buffer
    S.htmlProperties.htmlWriter(Group(xhtml), w)
    val cmd = ("cmd" -> "preview") ~ ("src" -> w.toString)
    ngMessage(cmd)
  }


  def makePage(x: Data): NodeSeq = {
    val errors = new ListBuffer[SanitizationEvent]
    val xhtml = TemplateParams.preview.withValue(true) {
      SanitizationChangeSupport.withSink(errors += _) {
        WikiRenderer.parseMarkdown(x.src, pageInfo.mkPath)
      }
    }
    xhtml
  }

  def save(in: JValue): Unit = {
    if (pageInfo == null) return
    val x = Extraction.extract[Data](in)
    val path = pageInfo.mkPath
    val id = WikiPageRecord where (_.path eqs path) orderDesc(_.datetime) select(_.id) get()
    val wpr = WikiPageRecord.createRecord
    wpr.editor(UserRecord.currentId).parent(id).path(path).datetime(now).source(x.src)
    wpr.comment(x.comment).size(x.src.length)
    wpr.save
    WikiLinkCache.update(path, true)
    partialUpdate(RedirectTo("/wiki/" + path))
  }

  override def receiveJson = {
    case obj: JObject =>
      val data = obj \ "data"
      (obj \ "cmd") match {
        case JString("update") =>
          update(data)
          JsCmds.Noop
        case JString("save") =>
          //save(data)
          self ! Save(data)
          JsCmds.Noop
        case _ =>
          logger.warn("invalid json: " + obj)
          JsCmds.Noop
      }
  }

  def ready(): Unit = {
    ngMessage("cmd" -> "ready")
  }

  def display(rec: WikiPageRecord): Unit = {
    val src = rec.source.is
    val msg = ("cmd" -> "display") ~ ( "data" -> ("src" -> src) )
    ngMessage(msg)
  }

  def loadAndDisplay(info: WikiPage): Unit = {
    val path = info.mkPath
    val page = WikiPageRecord where (_.path eqs path) orderDesc(_.datetime) get()
    page match {
      case None => //empty page
        ready()
      case Some(p) =>
        pid = p.id.is
        display(p)
    }
  }

  override def lowPriority = {
    case page: WikiPage =>
      logger.debug("got wiki page: " + page)
      pageInfo = page
      if (loaded)
        loadAndDisplay(page)
    case Save(data) => save(data)
  }
}
