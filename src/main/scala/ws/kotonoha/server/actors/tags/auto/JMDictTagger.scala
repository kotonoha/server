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
    case s if s.startsWith("v5") => List("v5", "verb")
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
    val prio = JMDictRecord.calculatePriority(strs)

    if (prio == 0) "nonfreq"
    else s"freq$prio"
  }

  //iru-eru regex
  val xru = ".*[いきしちにひみりぎじぢびぴえけせてねべめれげぜでべぺ]る".r

  def resolve(wr: String, rd: Option[String]): Unit = {
    val c = Candidate(wr, rd, None)
    val ji = JMDictRecord.forCandidate(c, 50)
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
        val godanEx = {
          val ends = r.reading.is.find {
            rd => xru.findFirstIn(rd.value.is).isDefined
          }.isDefined
          val tag = p1.contains("v5r")
          if (ends && tag) List("v5-ex") else Nil
        }
        (prio :: p1 ++ p2 ++ p3 ++ godanEx).flatMap(e => JMDictTagger.process(e)).distinct
    }
    sender ! PossibleTags(tags)
  }

  def receive = {
    case PossibleTagRequest(wr, rd) => resolve(wr, rd)
  }
}
