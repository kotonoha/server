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

import javax.inject.Inject

import akka.actor.ActorLogging
import akka.pattern.ask
import akka.util.Timeout
import net.liftweb.common.{Box, Empty}
import org.bson.types.ObjectId
import ws.kotonoha.akane.unicode.{KanaUtil, UnicodeUtil}
import ws.kotonoha.server.actors.dict.DictType._
import ws.kotonoha.server.actors.dict.{DictQuery, ExampleEntry, ExampleIds, LoadExamples, SearchResult, TranslationsWithLangs, _}
import ws.kotonoha.server.actors.tags.auto.{PossibleTagRequest, PossibleTags, WordAutoTagger}
import ws.kotonoha.server.actors.{SearchQuery, UserScopedActor}
import ws.kotonoha.server.ioc.IocActors
import ws.kotonoha.server.records._
import ws.kotonoha.server.records.events.AddWordRecord
import ws.kotonoha.server.util.{DateTimeUtils, LangUtil}
import ws.kotonoha.server.web.comet.TimeoutException

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

/**
 * @author eiennohito
 * @since 22.10.12 
 */

case class DictData(name: String, data: Seq[DictCard])

case class ExampleForSelection(ex: String, translation: Box[String], id: Long)

case class WordData(dicts: Seq[DictData], examples: List[ExampleForSelection], word: WordRecord, onSave: Promise[WordData], init: AddWordRecord)

case class DictCard(writing: String, reading: String, meaning: String)

object DictCard {
  def makeCard(writing: Seq[String], reading: Seq[String], meaning: Seq[String]) = {
    DictCard(
      writing.mkString(", "),
      reading.mkString(", "),
      meaning.mkString("\n")
    )
  }
}


case class CreateWordData(in: AddWordRecord)

class WordCreateActor @Inject() (
  ioc: IocActors
) extends UserScopedActor with ActorLogging {

  import DateTimeUtils._
  import akka.pattern.pipe

  implicit val timeout: Timeout = 10.seconds

  lazy val tagger = context.actorOf(ioc.props[WordAutoTagger], "tagger")

  def firstElem(s: String) = {
    val parts = s.split("[,、･]")
    parts.head
  }

  def prepareWord(rec: AddWordRecord): Future[WordData] = {
    val wr = firstElem(rec.writing.get)
    val rd: Option[String] = rec.reading.valueBox.map(firstElem)
    val jf = (userActor ? DictQuery(jmdict, wr, rd, 5)).mapTo[SearchResult]
    val wf = (userActor ? DictQuery(warodai, wr, rd, 5)).mapTo[SearchResult]
    val exs = jf.flatMap { jmen =>
      val en = jmen.entries
      val idsf = if (en.isEmpty) {
        services ? SearchQuery(wr)
      } else {
        services ? SearchQuery(wr + " " + en.head.readings.head)
      }
      idsf.mapTo[List[Long]].map(_.distinct).flatMap {
        exIds => {
          val trsid = (userActor ? TranslationsWithLangs(exIds, LangUtil.langs)).mapTo[List[ExampleIds]]
          val exs = trsid.flatMap(userActor ? LoadExamples(_)).mapTo[List[ExampleEntry]]
          exs.map(_.map {
            e => {
              ExampleForSelection(e.jap.content.get, e.other match {
                case x :: _ => x.content.valueBox
                case _ => Empty
              }, e.jap.id.get)
            }
          })
        }
      }
    }
    val tags = jf.flatMap {
      jm =>
        val req = jm.entries.headOption match {
          case Some(e) =>
            (e.writings.headOption, e.readings.headOption) match {
              case (Some(w), r) => Some(PossibleTagRequest(w, r))
              case (None, o@Some(r)) => Some(PossibleTagRequest(r, o))
              case (None, None) => None //rly wtf dude
            }
          case None =>
            val wx = firstElem(rec.writing.is)
            val rx: Option[String] = rec.reading.valueBox.map(firstElem)
            Some(PossibleTagRequest(wx, rx))
        }
        req match {
          case Some(s) => (tagger ? s).mapTo[PossibleTags]
          case None => Future.successful(PossibleTags(Nil))
        }
    }

    for (
      sr1 <- jf;
      sr2 <- wf;
      ex <- exs;
      t <- tags
    ) yield {
      val dicts = List(collapse(sr1, "JMDict"), collapse(sr2, "Warodai"))
      log.debug("Calculated word data")
      val onSave = Promise[WordData]()
      val canc = context.system.scheduler.scheduleOnce(15 minutes)(() => onSave.tryComplete(scala.util.Failure(new TimeoutException)))
      onSave.future.foreach(_ => canc.cancel())
      WordData(dicts, ex, createWord(rec.user.is, dicts, ex, t), onSave, rec)
    }
  }


  def collapse(in: SearchResult, name: String) = {
    import DictCard.makeCard
    import UnicodeUtil.{isKatakana => isk}
    DictData(name,
      in.entries.map {
        //if there is no writing and has katakana-only elems
        //then we make an entry (kana, hira from kata, meaning)
        case DictionaryEntry(Nil, rd, mn) if rd.exists(isk) => {
          makeCard(rd, rd.filter(isk).map(KanaUtil.kataToHira), mn)
        }
        case DictionaryEntry(wr, rd, mn) => makeCard(wr, rd, mn)
      }
    )
  }

  def createWord(user: ObjectId, dictData: List[DictData],
                 examples: List[ExampleForSelection], tags: PossibleTags): WordRecord = {
    val rec = WordRecord.createRecord
    rec.user(user).status(WordStatus.New).createdOn(now)
    rec.tags(tags.tags.toList)

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

  override def receive = {
    case CreateWordData(awr) => createWordData(awr)
    case r: PossibleTagRequest => tagger.forward(r)
  }
}
