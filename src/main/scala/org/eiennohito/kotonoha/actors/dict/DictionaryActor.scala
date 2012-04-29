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

package org.eiennohito.kotonoha.actors.dict

import akka.pattern.{ask, pipe}
import org.eiennohito.kotonoha.actors.DictionaryMessage
import akka.actor.{Props, Actor, ActorRef}
import org.eiennohito.kotonoha.util.LangUtil
import org.eiennohito.kotonoha.records.dictionary.{WarodaiRecord, JMDictRecord}
import org.eiennohito.kotonoha.dict.WarodaiBodyParser
import util.parsing.input.CharSequenceReader

/**
 * @author eiennohito
 * @since 29.04.12
 */

case class DictionaryEntry(writings: List[String], readings: List[String], meanings: List[String])
case class SearchResult(entries: List[DictionaryEntry])

object DictType extends Enumeration {
  val jmdict, warodai = Value
  type DictType = Value
}

case class DictQuery(dict: DictType.DictType, query: String, max: Int) extends DictionaryMessage
private case class Query(query: String, max: Int)

class DictionaryActor extends Actor {
  protected def receive = {
    case DictQuery(DictType.jmdict, q, max) => context.actorOf(Props[JMDictQActor]).forward(Query(q, max))
    case DictQuery(DictType.warodai, q, max) => context.actorOf(Props[WarodaiQActor]).forward(Query(q, max))
  }
}

class JMDictQActor extends Actor {
  def process(q: String, max: Int): SearchResult = {
    val objs = JMDictRecord.query(q, max)
    val entrs = objs.map { j =>
      val wrs = j.writing.is.flatMap {s => s.value.valueBox}
      val rds = j.reading.is.flatMap {s => s.value.valueBox}
      val mns = j.meaning.is.map { mn =>
        mn.pos.valueBox.map(_.toString() + ": ").openOr("") + mn.vals.is.filter(s => LangUtil.okayLang(s.loc)).map(_.str).mkString("; ")
      }
      DictionaryEntry(wrs, rds, mns)
    }
    SearchResult(entrs)
  }

  protected def receive = {
    case Query(q, max) => {
      sender ! process(q, max)
      context.stop(self)
    }
  }
}

class WarodaiQActor extends Actor {
  def process(q: String, max: Int): SearchResult = {
    val objs = WarodaiRecord.query(q, max)
    val builder = new StringBuilder(1024)
    val entrs = objs.map {w =>
      val rds = w.readings.is
      val wrs = w.writings.is
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

  protected def receive = {
    case Query(q, max) => {
      sender ! process(q, max)
      context.stop(self)
    }
  }
}
