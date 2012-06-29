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

case class DictQuery(dict: DictType.DictType, writing: String, reading: Option[String], max: Int) extends DictionaryMessage
private case class Query(writing: String, reading: Option[String], max: Int)

class DictionaryActor extends Actor {
  protected def receive = {
    case DictQuery(DictType.jmdict, w, r, max) => context.actorOf(Props[JMDictQActor]).forward(Query(w, r, max))
    case DictQuery(DictType.warodai, w, r, max) => context.actorOf(Props[WarodaiQActor]).forward(Query(w, r, max))
  }
}

class JMDictQActor extends Actor {
  def process(w: String, r: Option[String], max: Int): SearchResult = {
    val objs = JMDictRecord.query(w, r, max)
    val entrs = objs.map { j =>
      val wrs = j.writing.is.flatMap {s => s.value.valueBox}
      val rds = j.reading.is.flatMap {s => s.value.valueBox}
      val mns = j.meaning.is.map { mn => {
        val info = mn.info.is match {
          case Nil => ""
          case l => l.mkString("(", ",", "):")
        }
        info + mn.vals.is.filter(s => LangUtil.okayLang(s.loc)).map(_.str).mkString("; ")
      }}
      DictionaryEntry(wrs, rds, mns)
    }
    SearchResult(entrs)
  }

  protected def receive = {
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
    case Query(w, r, max) => {
      sender ! process(w, r, max)
      context.stop(self)
    }
  }
}
