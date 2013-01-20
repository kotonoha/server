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

package ws.kotonoha.server.actors.tags.auto

import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.web.comet.Candidate
import ws.kotonoha.server.records.dictionary.{JMString, JMDictRecord}
import net.liftweb.mongodb.Limit

/**
 * @author eiennohito
 * @since 20.01.13 
 */

object JMDictTagger {
  val jdictAliases = Map(
    "" -> "unknown"
  )

  val add1: PartialFunction[String, List[String]] = {
    case s if s.startsWith("v5") => List("verb", "v5")
    case s if s.startsWith("v") => List("verb")
  }

  val add2 = Map(
    "adj-i" -> List("adj"),
    "adj-na" -> List("adj")
  )

  val additional = add1 orElse add2

  def process(t: String) = {
    val norm = jdictAliases.get(t) match {
      case Some(x) => x
      case None => t
    }

    if (!additional.isDefinedAt(norm))
      norm :: Nil
    else norm :: additional(norm)
  }
}

class JMDictTagger extends UserScopedActor {

  import ws.kotonoha.server.util.KBsonDSL._

  def resolvePriority(strs: List[JMString]): String = {
    val x = strs.flatMap {
      _.priority.is.map {
        _.value
      }
    }
      .map {
      case "news1" => 1
      case "news2" => 2
      case "ichi1" => 1
      case "ichi2" => 2
      case "spec1" => 1
      case "spec2" => 2
      case "gai1" => 1
      case "gai2" => 2
      case s if s.startsWith("nf") => (s.substring(2).toInt / 24) + 1
      case _ => 0
    }

    val sum = x.fold(0)(_ + _)
    val cnt = x.length

    if (cnt == 0) "nonfreq"
    else {
      val avg = sum / cnt
      s"freq$avg"
    }
  }

  def resolve(wr: String, rd: Option[String]): Unit = {
    val c = Candidate(wr, rd, None)
    val jv = c.toQuery
    val ji = JMDictRecord.findAll(jv, Limit(50))
    val tags = ji.headOption.toList.flatMap {
      r =>
        val p1 = r.meaning.is.flatMap {
          _.info.is
        }
        val p2 = r.writing.is.flatMap {
          _.info.is
        }
        val p3 = r.reading.is.flatMap {
          _.info.is
        }
        val prio = resolvePriority(r.writing.is)
        (prio :: p1 ++ p2 ++ p3).flatMap(e => JMDictTagger.process(e)).distinct
    }
    sender ! PossibleTags(tags)
  }

  def receive = {
    case PossibleTagRequest(wr, rd) => resolve(wr, rd)
  }
}
