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

import akka.actor.{ActorLogging, Actor}
import ws.kotonoha.server.records._
import akka.dispatch.{ExecutionContext, DefaultPromise, Future, Promise}
import net.liftweb.common.{Empty, Box}
import ws.kotonoha.server.actors.dict.DictType._
import ws.kotonoha.server.actors.RootActor
import ws.kotonoha.server.web.comet.TimeoutException
import akka.util.{Timeout, Duration}
import ws.kotonoha.server.actors.dict._
import ws.kotonoha.server.util.{DateTimeUtils, LangUtil}

import akka.pattern.{ask, pipe}
import akka.util.duration._
import scala.Left
import ws.kotonoha.server.actors.dict.DictQuery
import ws.kotonoha.server.actors.dict.TranslationsWithLangs
import scala.Some
import ws.kotonoha.server.actors.dict.LoadExamples
import ws.kotonoha.server.actors.SearchQuery
import ws.kotonoha.server.actors.dict.ExampleIds
import ws.kotonoha.server.actors.dict.SearchResult
import ws.kotonoha.server.actors.dict.ExampleEntry
import ws.kotonoha.akane.unicode.{KanaUtil, UnicodeUtil}
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 22.10.12 
 */

case class DictData(name: String, data: List[DictCard])
case class ExampleForSelection(ex: String, translation: Box[String], id: Long)
case class WordData(dicts: List[DictData], examples: List[ExampleForSelection], word: WordRecord, onSave: Promise[WordData])

case class DictCard(writing: String, reading: String, meaning: String)
object DictCard {
  def makeCard(writing: List[String], reading: List[String], meaning: List[String]) = {
    DictCard(
      writing.mkString(", "),
      reading.mkString(", "),
      meaning.mkString("\n")
    )
  }
}


case class CreateWordData(in: AddWordRecord)

class WordCreateActor extends Actor with RootActor with ActorLogging {
  import DateTimeUtils._
  implicit val timeout: Timeout = 10 seconds
  implicit val ec: ExecutionContext = context.system.dispatcher

  def prepareWord(rec: AddWordRecord): Future[WordData] = {
    val wr = rec.writing.is
    val rd: Option[String] = rec.reading.valueBox
    val jf = (root ? DictQuery(jmdict, wr, rd, 5)).mapTo[SearchResult]
    val wf = (root ? DictQuery(warodai, wr, rd, 5)).mapTo[SearchResult]
    val exs = jf.flatMap { jmen => {
      val idsf = jmen.entries match {
        case Nil => { //don't have such word in dictionary
          root ? SearchQuery(wr)
        }
        case x :: _ => {
          root ? SearchQuery(wr + " " + x.readings.head)
        }
      }
      idsf.mapTo[List[Long]].map(_.distinct).flatMap { exIds => {
        val trsid = (root ? TranslationsWithLangs(exIds, LangUtil.langs)).mapTo[List[ExampleIds]]
        val exs = trsid.flatMap(root ? LoadExamples(_)).mapTo[List[ExampleEntry]]
        exs.map (_.map{ e => {
           ExampleForSelection(e.jap.content.is, e.other match {
             case x :: _ => x.content.valueBox
             case _ => Empty
           }, e.jap.id.is)
        }})
      }}
    }}

    jf.zip(wf).zip(exs) map {
      case ((sr1, sr2), ex) => {
        val dicts = List(collapse(sr1, "JMDict"), collapse(sr2, "Warodai"))
        log.debug("Calculated word data")
        val onSave = new DefaultPromise[WordData]()
        val d: Duration = (15 minutes)
        context.system.scheduler.scheduleOnce(d)(() => onSave.tryComplete(Left(new TimeoutException)))
        WordData(dicts, ex, createWord(rec.user.is, dicts, ex), onSave)
      }
    }
  }



  def collapse(in: SearchResult, name: String) = {
    import DictCard.makeCard
    import UnicodeUtil.{isKatakana => isk}
    DictData( name,
      in.entries.map ({
        //if there is no writing and has katakana-only elems
        //then we make an entry (kana, hira from kata, meaning)
        case DictionaryEntry(Nil, rd, mn) if rd.exists(isk(_)) => {
          makeCard(rd, rd.filter(isk(_)).map(KanaUtil.kataToHira(_)), mn)
        }
        case DictionaryEntry(wr, rd, mn) => makeCard(wr, rd, mn)
      })
    )
  }

  def createWord(user: ObjectId, dictData: List[DictData], examples: List[ExampleForSelection]): WordRecord = {
    val rec = WordRecord.createRecord
    rec.user(user).status(WordStatus.New).createdOn(now)

//    val exs = examples.map { e =>
//      ExampleRecord.createRecord.id(e.id).example(e.ex).translation(e.translation.openOr(""))
//    }
//    rec.examples(exs)

    val data = dictData.flatMap(d => d.data).headOption
    data match {
      case Some(d) => rec.writing(d.writing).reading(d.reading).meaning(d.meaning)
      case _ => rec.writing("").reading("").meaning("")
    }
    rec
  }

  def createWordData(awr: AddWordRecord) {
    val item = prepareWord(awr)
    item pipeTo sender
  }

  protected def receive = {
    case CreateWordData(awr) => createWordData(awr)
  }
}
