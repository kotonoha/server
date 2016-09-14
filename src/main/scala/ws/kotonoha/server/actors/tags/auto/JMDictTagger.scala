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

package ws.kotonoha.server.actors.tags.auto

import com.google.inject.Inject
import ws.kotonoha.akane.dic.jmdict.{CommonInfo, JMDictUtil, JmdictTag, JmdictTagMap}
import ws.kotonoha.akane.dic.lucene.jmdict.LuceneJmdict
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.actors.model.Candidate

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

  def process(t: JmdictTag) = {
    val norm = t match {
      case JmdictTag.Unrecognized(_) => "unknown"
      case _ => JmdictTagMap.tagInfo(t.value).repr
    }

    if (!additional.isDefinedAt(norm))
      norm :: Nil
    else norm :: additional(norm)
  }
}

class JMDictTagger @Inject() (
  jmd: LuceneJmdict
) extends UserScopedActor {

  def resolvePriority(strs: Seq[CommonInfo]): String = {
    val prio = JMDictUtil.calculatePriority(strs)

    if (prio == 0) "nonfreq"
    else s"freq${3 - prio}" // prio: 2 -> 1, 1 -> 2
  }

  //iru-eru regex
  val xru = ".*[いきしちにひみりぎじぢびぴえけせてねべめれげぜでべぺ]る".r

  def resolve(wr: String, rd: Option[String]): Unit = {
    val c = Candidate(wr, rd, None)
    val sres = jmd.find(c.query(limit = 1))
    val ji = sres.data
    val tags = ji.headOption.toList.flatMap {
      r =>
        val p1 = r.meanings.flatMap {
          x => x.pos ++ x.info
        }
        val p2 = r.writings.flatMap { _.info }
        val p3 = r.readings.flatMap { _.info}
        val prio = resolvePriority(r.writings)
        val godanEx = {
          val ends = r.readings.exists {
            rd => xru.findFirstIn(rd.content).isDefined
          }
          val tag = p1.contains(JmdictTag.v5r)
          if (ends && tag) List(JmdictTag.v5rI) else Nil
        }
        Seq(prio) ++ (p1 ++ p2 ++ p3 ++ godanEx).flatMap(e => JMDictTagger.process(e)).distinct
    }
    sender ! PossibleTags(tags)
  }

  def receive = {
    case PossibleTagRequest(wr, rd) => resolve(wr, rd)
  }
}
