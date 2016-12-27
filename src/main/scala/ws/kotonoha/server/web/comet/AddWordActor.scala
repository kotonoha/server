/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import com.google.inject.Inject
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.http.js.JsCmds.{RedirectTo, _Noop}
import net.liftweb.json.JsonAST.{JField, JObject, JString, JValue}
import net.liftweb.json.{DefaultFormats, Extraction}
import org.bson.types.ObjectId
import ws.kotonoha.akane.dic.jmdict.JmdictEntry
import ws.kotonoha.akane.dic.lucene.jmdict.LuceneJmdict
import ws.kotonoha.server.actors.ForUser
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import ws.kotonoha.server.actors.learning.ProcessMarkEvents
import ws.kotonoha.server.actors.lift.pertab.NamedCometActor
import ws.kotonoha.server.actors.lift.{AkkaInterop, NgLiftActor}
import ws.kotonoha.server.actors.model.Candidate
import ws.kotonoha.server.actors.tags.TagParser
import ws.kotonoha.server.records.events.{AddWordRecord, MarkEventRecord}
import ws.kotonoha.server.records.{UserRecord, WordCardRecord, WordRecord}
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.server.util.parsing.AddStringParser

import scala.collection.Map
import scala.util.parsing.input.CharSequenceReader

/**
 * @author eiennohito
 * @since 19.01.13 
 */

case class InvalidStringException(str: String) extends Exception("String " + str + " is not valid")

case class Possibility(writing: String, reading: String, meaning: Seq[String], id: Option[String] = None)

case class DisplayingEntry(item: Candidate, present: Seq[Possibility], dic: Seq[Possibility])

case class PresentEntry(id: ObjectId, writing: List[String], reading: List[String], meaning: String)

class AddWordActor @Inject() (
  jmd: LuceneJmdict
) extends NgLiftActor with AkkaInterop with NamedCometActor with Logging with ReleaseAkka {

  import net.liftweb.{json => j}
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.KBsonDSL._

  val self = this

  private val uid = UserRecord.currentId.openOrThrowException("This page shouldn't be accessible without logging in")

  private var everything: List[Candidate] = Nil
  private var good: List[Candidate] = Nil
  private var present: Map[String, List[PresentEntry]] = Map.empty

  def svcName = "AddWordActor"

  override def receiveJson = {
    case x => self ! ProcessJson(x); _Noop
  }

  private def all(s: String): List[Candidate] = AddStringParser.entries(new CharSequenceReader(s)) match {
    case AddStringParser.Success(l, _) => l.distinct
    case _ => logger.warn(s"can't parse $s"); Nil
  }


  def handleSave(cands: List[Candidate], tagOps: JValue): Unit = {
    if (cands.isEmpty)
      return
    val ops = TagParser.parseOps(tagOps)
    val lid = new ObjectId()
    cands.map {
      c =>
        val r = AddWordRecord.createRecord
        r.writing(c.writing)
        r.reading(c.reading)
        r.meaning(c.meaning)
        r.processed(false)
        r.group(lid)
        r.user(uid)
        r.tags(ops.ops)
        r.source("default")
        r.save()
    }
    present = Map.empty
    partialUpdate(RedirectTo(s"/words/approve_added?list=$lid"))
  }

  private def handleInit() = {
    logger.debug(s"loading words for user $uid")
    val q = WordRecord.where(_.user eqs uid).select(_.id, _.writing, _.reading, _.meaning)
    val data = q.fetch().map(PresentEntry.tupled)

    var map = Map.empty[String, List[PresentEntry]]
    for {
      entry <- data
      wr <- entry.writing
    } {
      val content = map.getOrElse(wr, Nil)
      map = map.updated(wr, entry :: content)
    }

    present = map
  }

  private def dicPossibiliry(dic: JmdictEntry) = {
    Possibility(
      writing = dic.writings.map(k => k.content).mkString(", "),
      reading = dic.readings.map(k => k.content).mkString(", "),
      meaning = dic.meanings.map(_.content.filter(_.lang == "eng").map(_.str)).map(_.mkString("; ")),
      id = Some(dic.id.toString)
    )
  }

  private def createEntry(in: Candidate, cur: Map[String, List[PresentEntry]]) = {
    val wds = cur.getOrElse(in.writing, Nil).map { r =>
      val wrs = r.writing.mkString(",")
      val rds = r.reading.mkString(",")
      Possibility(wrs, rds, r.meaning :: Nil, Some(r.id.toHexString))
    }
    val dics = jmd.find(in.query(limit = 3)).data.map(dicPossibiliry)
    DisplayingEntry(in, wds, dics)
  }

  def processAddData(input: String): Unit = {
    everything = all(input)
    if (everything.isEmpty) {
      good = Nil
      updateHttpVariants(Nil)
    } else {
      val alreadyPresent = present
      good = everything.filter {
        k => !alreadyPresent.contains(k.writing)
      }
      val data = everything map {
        createEntry(_, alreadyPresent)
      }
      updateHttpVariants(data)
    }
  }


  def updateHttpVariants(data: List[DisplayingEntry]) {
    val js = Extraction.decompose(data)(DefaultFormats)
    ngMessageRaw(("cmd" -> "update") ~ ("data" -> js))
  }

  def handlePublish(data: JValue): Unit = {
    data match {
      case JString(s) => processAddData(s)
      case s => logger.warn(s"Invalid js from ng: ${j.compactRender(s)}")
    }
  }

  def penaltize(jv: JValue): Unit = {
    implicit val formats = DefaultFormats
    val c = Extraction.extract[DisplayingEntry](jv)
    val ids = c.present.flatMap(p => p.id).map {
      new ObjectId(_)
    }
    val recs: List[WordCardRecord] = WordCardRecord where (_.user eqs uid) and (_.word in ids) fetch()
    val evs = recs.map {
      r =>
        val mer = MarkEventRecord.createRecord
        mer.user(uid)
        mer.card(r.id.get)
        mer.mode(r.cardMode.get)
        mer.datetime(DateTimeUtils.now)
        mer.mark(1)
        mer.client("add-form")
        mer.time(0)
    }

    akkaServ ! ForUser(uid, ProcessMarkEvents(evs))
  }

  private def fromPage(cmd: String, jv: JValue): Unit = {
    cmd match {
      case "init" => handleInit()
      case "publish" => handlePublish(jv)
      case "save_all" => handleSave(everything, jv)
      case "save_good" => handleSave(good, jv)
      case "penaltize" => penaltize(jv)
    }
  }

  def process(jv: JValue): Unit = jv match {
    case JObject(JField("cmd", JString(cmd)) :: JField("data", jv) :: Nil) => fromPage(cmd, jv)
    case x =>
      def js = j.compactRender(x)
      logger.warn(s"Invalida js from ng: $js")
  }

  override def lowPriority = {
    case ProcessJson(jv) => process(jv)
  }
}
