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

package ws.kotonoha.server.actors.dict

import akka.actor.{Actor, Props}
import com.google.inject.Inject
import org.apache.lucene.search.BooleanClause.Occur
import ws.kotonoha.akane.dic.jmdict.JmdictTagMap
import ws.kotonoha.dict.jmdict.{JmdictQuery, JmdictQueryPart, LuceneJmdict}
import ws.kotonoha.server.actors.DictionaryMessage
import ws.kotonoha.server.dict.WarodaiBodyParser
import ws.kotonoha.server.ioc.IocActors
import ws.kotonoha.server.records.dictionary.WarodaiRecord
import ws.kotonoha.server.util.LangUtil

import scala.util.parsing.input.CharSequenceReader

/**
 * @author eiennohito
 * @since 29.04.12
 */

case class DictionaryEntry(writings: Seq[String], readings: Seq[String], meanings: Seq[String])
case class SearchResult(entries: Seq[DictionaryEntry])

object DictType extends Enumeration {
  val jmdict, warodai = Value
  type DictType = Value
}

case class DictQuery(dict: DictType.DictType, writing: String, reading: Option[String], max: Int) extends DictionaryMessage
private case class Query(writing: String, reading: Option[String], max: Int)

class DictionaryActor @Inject()(ioc: IocActors) extends Actor {
  override def receive = {
    case DictQuery(DictType.jmdict, w, r, max) => context.actorOf(ioc.props[JMDictQActor]).forward(Query(w, r, max))
    case DictQuery(DictType.warodai, w, r, max) => context.actorOf(Props[WarodaiQActor]).forward(Query(w, r, max))
  }
}

class JMDictQActor @Inject() (jmd: LuceneJmdict) extends Actor {
  def process(w: String, r: Option[String], max: Int): SearchResult = {
    val jq = JmdictQuery(
      writings = Seq(JmdictQueryPart(w, Occur.MUST)),
      readings = r.map(rx => JmdictQueryPart(rx, Occur.MUST)).toSeq,
      limit = max
    )
    val entries = jmd.find(jq)
    val entrs = entries.data.map { j =>
      val wrs = j.writings.map(_.content)
      val rds = j.readings.map(_.content)
      val mns = j.meanings.map { m =>
        val infos = m.pos ++ m.info
        val info = if (infos.isEmpty) "" else infos.map(i => JmdictTagMap.tagInfo(i.value).repr).mkString("(", ",", ") ")
        info + m.content.filter(c => LangUtil.okayLang(c.lang)).map(_.str).mkString("; ")
      }
      DictionaryEntry(wrs, rds, mns)
    }

    SearchResult(entrs)
  }

  override def receive = {
    case Query(w, r, max) => {
      sender ! process(w, r, max)
      context.stop(self)
    }
  }
}

class WarodaiQActor extends Actor {
  def process(w: String, r: Option[String], max: Int): SearchResult = {
    val objs = WarodaiRecord.query(w, r, max)
    val builder = new StringBuilder(1024)
    val entrs = objs.map {w =>
      val rds = w.readings.get
      val wrs = w.writings.get
      val mns = {
        val body = WarodaiBodyParser.body(new CharSequenceReader(w.body.is))
        if (body.successful) {
          builder.clear()
          body.get.mkString(builder)
          builder.toString()
        } else {
          "Error: " + body
        }
      }
      DictionaryEntry(wrs, rds, List(mns))
    }
    SearchResult(entrs)
  }

  override def receive = {
    case Query(w, r, max) => {
      sender ! process(w, r, max)
      context.stop(self)
    }
  }
}
