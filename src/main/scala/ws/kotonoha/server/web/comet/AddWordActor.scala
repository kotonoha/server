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

import ws.kotonoha.server.actors.lift.{NgLiftActor, AkkaInterop}
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import akka.util.Timeout
import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import ws.kotonoha.server.records._
import com.weiglewilczek.slf4s.Logging
import net.liftweb.common.{Empty, Box}
import ws.kotonoha.server.actors.model._
import net.liftweb.http.CometActor
import net.liftweb.json.JsonAST._
import net.liftweb.http.js.JsCmds
import akka.actor.{PoisonPill, Props}
import scala.Left
import scala.Some
import ws.kotonoha.server.actors.{SaveRecord, UpdateRecord}
import net.liftweb.common.Full
import scala.Right
import ws.kotonoha.server.actors.model.WordData
import net.liftweb.json.{DefaultFormats, Extraction, NoTypeHints, Serialization}
import akka.dispatch.{ExecutionContext, Promise, Future}
import ws.kotonoha.server.actors.interop.ParseSentence
import ws.kotonoha.akane.ParsedQuery
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.akane.juman.JumanUtil
import net.liftweb.json.ext.JodaTimeSerializers


/**
 * @author eiennohito
 * @since 29.04.12
 */



case class WordList(id: Long)
case object PrepareWords
case object PublishNext
case class WordCount(current: Int, total: Int)
case class DoRenderAndDisplay(wd: WordData)
case object Cleanup

class TimeoutException extends RuntimeException

trait AddWordActorT extends NamedCometActor with NgLiftActor with AkkaInterop with Logging {
  lazy val root = akkaServ.root
  val self = this
  import akka.util.duration._
  import akka.pattern.ask
  import com.foursquare.rogue.Rogue._
  import ws.kotonoha.server.util.KBsonDSL._

  private implicit val timeout = Timeout(10 seconds)
  private var list: Box[WordList] = Empty
  private var selector: WordDataCalculator = _

  implicit val ec: ExecutionContext = akkaServ.context

  lazy val wordCreator = createActor(Props[WordCreateActor])

  override def localShutdown {
    wordCreator ! PoisonPill
    super.localShutdown
  }

  def prepareWord(in: AddWordRecord) = {
    val f = (wordCreator ? CreateWordData(in)).mapTo[WordData]
    f
  }

  def svcName = "AddSvc"

  override def render = {
    logger.debug("AddWordActor.render")
    val pr = super.render.copy(xhtml = Full(defaultHtml))
    pr
  }

  var displaying: Map[String, WordData] = Map()

  private class WordDataCalculator extends Logging {
    import DateTimeUtils._
    private val uid = UserRecord.currentId.get

    private def q = list match {
      case Full(l) => {
        AddWordRecord where (_.user eqs uid) and (_.group eqs l.id) and (_.processed eqs false)
      }
      case _ => {
        AddWordRecord where (_.user eqs uid) and (_.processed eqs false)
      }
    }

    private var items = q.fetch()
    private var cur_ = 0
    def cur = cur_

    private var total_ = items.size

    def total = total_

    def next = {
      items match {
        case Nil => None
        case _ => {
          val f = calculate(items.head)
          cur_ += 1
          items = items.tail
          Some(f)
        }
      }
    }

    protected def calculate(item: AddWordRecord) =
    prepareWord(item) map (w => {
      w.word.tags(item.tags.is).writing(item.writing.valueBox).user(item.user.valueBox)
      item.reading.valueBox map {r => w.word.reading(r) }
      item.meaning.valueBox map {m => w.word.meaning(m) }
      w.onSave foreach {
        i => root ! UpdateRecord(item.processed(true))
      }
      w
    })

    def addWordCandidate(wc: DictCard) = {
      val awr = AddWordRecord.createRecord
      awr.user(uid)
      awr.datetime(now)
      awr.group(list map {_.id})
      awr.writing(wc.writing)
      awr.reading(wc.reading)
      awr.meaning(wc.meaning)
      total_ += 1
      items = awr :: items
      root ! SaveRecord(awr)
      updateIndices(cur, total)
    }
  }

  def updateIndices(cur: Int, total: Int): Unit = {
    ngMessage(
      ("cmd" -> "update-indices") ~
      ("total" -> total) ~
      ("cur" -> cur)
    )
  }

  def prepare() = {
    logger.debug("prepare is called")
    selector = new WordDataCalculator()
    self ! PublishNext
    self ! PublishNext
  }


  def locateNext(): Unit = {
    logger.debug("Trying to display next word!")
    val n = selector.next
    n match {
      case Some(f) => {
        f.onComplete {
          case Right(wd) => self ! DoRenderAndDisplay(wd)
          case Left(e) => logger.error("Error in displaying word", e); self ! PublishNext
        }
      }
      case None => {
        ngMessage("cmd" -> "no-items")
      }
    }
  }

  def saveWord(jobj: JObject, status: String) = {
    val st = status match {
      case "approved" => WordStatus.Approved
      case "new" => WordStatus.New
      case _ => WordStatus.ReviewWord
    }
    val wid = jobj \ "_id" match {
      case JString(id) => id
      case _ => ""
    }
    val wi = displaying.get(wid)
    if (wi.isDefined) {
      val w = wi.get
      val wd = w.word
      val filtered = WordRecord.trimInternal(jobj, out = false)
      wd.setFieldsFromJValue(filtered)
      w.onSave.foreach { x => root ! RegisterWord(wd, st) }
      w.onSave.tryComplete(Right(w))
    }
    self ! PublishNext
  }

  def skipWord(wid: String): Unit = {
    displaying.get(wid) map { w => w.onSave.tryComplete(Right(w)) }
    self ! PublishNext
  }

  def process(js: JValue): Unit = {
    implicit val formats = DefaultFormats ++ JodaTimeSerializers.all
    (js \ "cmd") match {
      case JString("save") => {
        ((js \ "word"), (js \ "status")) match {
          case (j: JObject, JString(stat)) => saveWord(j, stat)
          case _ => logger.info("Invalid save command:" + js); self ! PublishNext
        }
      }
      case JString("skip") => {
        (js \ "wid") match {
          case JString(wid) => skipWord(wid)
          case _ => logger.info("Invalid skip command"); self ! PublishNext
        }
      }
      case JString("add-from-dic") => {
        val jv = (js \ "entry")
        val o = Extraction.extract[DictCard](jv)
        selector.addWordCandidate(o)
      }
      case _ => logger.info("Invalid message " + js)
    }
  }

  case class ProcessJson(obj: JValue)

  override def receiveJson = {
    case o => self ! ProcessJson(o); JsCmds.Noop
  }

  case class RemoveItem(s: String)

  implicit val formats = Serialization.formats(NoTypeHints)

  def renderExample(exs: ExampleForSelection, word: WordRecord): Future[JValue] = {
    val ex = exs.ex

    def selected: Future[Boolean] = {
      val res = word.reading.is.exists { rd => ex.contains(rd) } ||
                word.writing.is.exists { wr => ex.contains(wr) }
      if (res) Promise.successful(true)
      else {
        val wrs = word.writing.is.toSet
        val f = (root ? ParseSentence(ex)).mapTo[ParsedQuery]
        f map (lst => {
          val jwr = lst.inner.flatMap{ i => i.writing :: i.dictForm :: JumanUtil.daihyouWriting(i).writing :: Nil }.toSet
          val opt = jwr.exists(wrs.contains(_))
          opt
        })
      }
    }
    selected map { s =>
      ("example" -> ex) ~
      ("translation" -> exs.translation.openOr("")) ~
      ("id" -> exs.id) ~
      ("selected" -> s)
    }
  }

  def renderAndPush(data: WordData) = {
    val hid = data.word.id.is.toString
    displaying = displaying + (hid -> data)
    data.onSave.onComplete { x => self ! RemoveItem(hid) }

    val dicts = Extraction.decompose(data.dicts)

    val jv = ("word" -> data.word.stripped) ~
             ("dics" -> dicts) ~
             ("cmd" -> "word") ~
             ("total" -> selector.total) ~
             ("cur" -> selector.cur)

    val fexs = data.examples.map(renderExample(_, data.word))
    val jvsf = Future.sequence(fexs)
    val exjs: Future[JValue] = jvsf map {jv =>  "word" -> ("examples" -> jv) }
    exjs foreach { d => ngMessage(jv.merge(d)) }
  }

  override def lowPriority = {
    case PrepareWords => list = Empty; prepare()
    case l: WordList => list = Full(l); prepare()
    case PublishNext => locateNext()
    case ProcessJson(js) => process(js)
    case DoRenderAndDisplay(wd) => renderAndPush(wd)
    case RemoveItem(hid) => displaying -= hid
  }

  override def highPriority = {
    case Cleanup => logger.info("Cleaning actor up"); displaying = Map()
  }
}

class AddWordActor extends CometActor with AddWordActorT with ReleaseAkka
