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

import ws.kotonoha.server.actors.lift.{AkkaInterop, NgLiftActor}
import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.js.JsCmds.{RedirectTo, _Noop}
import net.liftweb.json.JsonAST.JValue
import com.typesafe.scalalogging.slf4j.Logging
import ws.kotonoha.server.util.parsing.AddStringParser
import util.parsing.input.CharSequenceReader
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.server.util.{DateTimeUtils, Strings}
import net.liftweb.json.JsonAST.JObject
import scala.Some
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JString
import ws.kotonoha.server.records.{WordCardRecord, WordRecord, UserRecord}
import ws.kotonoha.server.records.events.{MarkEventRecord, AddWordRecord}
import ws.kotonoha.server.actors.tags.TagParser
import org.bson.types.ObjectId
import ws.kotonoha.server.records.dictionary.JMDictRecord
import net.liftweb.json.{DefaultFormats, Extraction}
import ws.kotonoha.server.learning.ProcessMarkEvents
import ws.kotonoha.server.actors.ForUser

/**
 * @author eiennohito
 * @since 19.01.13 
 */

case class InvalidStringException(str: String) extends Exception("String " + str + " is not valid")

case class Candidate(writing: String, reading: Option[String], meaning: Option[String]) {
  def toQuery: JValue = {
    import ws.kotonoha.server.util.KBsonDSL._
    //def regex(s: String) = "$regex" -> ("^" + s + "$")

    ("writing.value" -> writing) ~ (reading map {
      r => ("reading.value" -> r)
    })
  }
}

case class Possibility(writing: String, reading: String, meaning: String, id: Option[String] = None)

case class DisplayingEntry(item: Candidate, present: List[Possibility], dic: List[Possibility])

object Candidate {

  import UnicodeUtil._

  def wrap(s1: String) = {
    val s = Strings.trim(s1)
    if (s == null || s.equals("")) {
      None
    } else {
      Some(s)
    }
  }

  def apply(in: String) = {
    in.split("[|｜]", 3) match {
      case Array(w) => new Candidate(w, None, None)
      case Array(w, r, m) => new Candidate(w, wrap(r), wrap(m))
      case Array(w, smt) => {
        if (stream(smt).forall(isKana(_))) {
          new Candidate(w, wrap(smt), None)
        } else {
          new Candidate(w, None, wrap(smt))
        }
      }
      case _ => throw new InvalidStringException(in)
    }
  }
}

trait AddWordActorT extends NgLiftActor with AkkaInterop with NamedCometActor with Logging {

  import ws.kotonoha.server.util.KBsonDSL._
  import com.foursquare.rogue.LiftRogue._
  import net.liftweb.{json => j}

  val self = this

  val uid = UserRecord.currentId.openOrThrowException("This page shouldn't be accessible without logging in")

  var everything: List[Candidate] = Nil
  var good: List[Candidate] = Nil

  def svcName = "AddWordActor"

  override def receiveJson = {
    case x => self ! ProcessJson(x); _Noop
  }

  def all(s: String): List[Candidate] = AddStringParser.entries(new CharSequenceReader(s)) match {
    case AddStringParser.Success(l, _) => l
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
        r.save
    }
    partialUpdate(RedirectTo(s"/words/approve_added?list=$lid"))
  }

  //load present words
  def calculatePresent() = {
    val wrs = everything.map {
      _.writing
    }
    val q = WordRecord where (_.user eqs uid) // and (_.writing in wrs)
    val int = q.fetch() flatMap {
        i => i.writing.is.map {
          w => w -> i
        }
      }
    int.foldLeft(Map[String, List[WordRecord]]().withDefaultValue(Nil)) {
      case (m, (w, i)) =>
        m.updated(w, i :: m(w))
    }
  }

  def findEntries() = {
    val patterns = everything.map {
      _.toQuery
    }
    val dicQ: JObject = "$or" -> patterns
    val dics = JMDictRecord.findAll(dicQ)

    val des = dics.map {
      de => de.writing.is.map {
        jms => jms.value.is -> List(de)
      }
    }.foldLeft(Map[String, List[JMDictRecord]]().withDefaultValue(Nil)) {
      (m1, m2) =>
        m2.foldLeft(m1) {
          case (m, (k, v)) => m.updated(k, (m(k) ++ v).distinct)
        }
    }
    des
  }

  def dicPossibiliry(dic: JMDictRecord) = {
    Possibility(
      writing = dic.writing.is.map(k => k.value.is).mkString(", "),
      reading = dic.reading.is.map(k => k.value.is).mkString(", "),
      meaning = dic.meaning.is.flatMap(_.vals.is.filter(_.loc == "eng").map(_.str)).headOption.getOrElse("")
    )
  }

  def createEntry(in: Candidate, cur: Map[String, List[WordRecord]], dic: Map[String, List[JMDictRecord]]) = {
    val wds = cur(in.writing).map(r => Possibility(r.writing.stris, r.reading.stris, r.meaning.is, Some(r.id.is.toString)))
    val dics = dic(in.writing).map(dicPossibiliry(_))
    DisplayingEntry(in, wds, dics)
  }

  def processAddData(input: String): Unit = {
    everything = all(input)
    if (everything.isEmpty) {
      good = Nil
      updateHttpVariants(Nil)
    } else {
      val alreadyPresent = calculatePresent()
      val dics = findEntries()
      good = everything.filter {
        k => alreadyPresent(k.writing).isEmpty
      }
      val data = everything map {
        createEntry(_, alreadyPresent, dics)
      }
      updateHttpVariants(data)
    }
  }


  def updateHttpVariants(data: List[DisplayingEntry]) {
    val js = Extraction.decompose(data)(DefaultFormats)
    ngMessage(("cmd" -> "update") ~ ("data" -> js))
  }

  def handlePublish(data: JValue): Unit = {
    data match {
      case JString(s) => processAddData(s)
      case s => logger.warn(s"Invalid js from ng: ${j.compact(j.render(s))}")
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
        mer.card(r.id.is)
        mer.datetime(DateTimeUtils.now)
        mer.mark(1)
        mer.time(0)
    }

    akkaServ ! ForUser(uid, ProcessMarkEvents(evs))
  }

  def fromPage(cmd: String, jv: JValue): Unit = {
    cmd match {
      case "publish" => handlePublish(jv)
      case "save_all" => handleSave(everything, jv)
      case "save_good" => handleSave(good, jv)
      case "penaltize" => penaltize(jv)
    }
  }

  def process(jv: JValue): Unit = jv match {
    case JObject(JField("cmd", JString(cmd)) :: JField("data", jv) :: Nil) => fromPage(cmd, jv)
    case x =>
      def js = j.compact(j.render(x))
      logger.warn(s"Invalida js from ng: $js")
  }

  override def lowPriority = {
    case ProcessJson(jv) => process(jv)
  }
}

class AddWordActor extends AddWordActorT with ReleaseAkka
