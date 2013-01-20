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
import ws.kotonoha.server.records.dictionary.JMDictRecord
import net.liftweb.mongodb.Limit

/**
 * @author eiennohito
 * @since 20.01.13 
 */

object JMDictInfo {
  val jdictAliases = Map(
    "" -> "unknown"
  )

  val add1: PartialFunction[String, List[String]] = {
    case s if s.startsWith("v") => List("verb")
    case s if s.startsWith("adj") => List("adj")
  }

  val add2 = Map(
    "" -> List("")
  )

  val additional = add1 orElse add2

  def process(t: String) = {
    val norm = jdictAliases.get(t) match {
      case Some(x) => x
      case None => t
    }

    if (additional.isDefinedAt(norm))
      norm :: Nil
    else norm :: additional(norm)
  }
}

class JMDictInfo extends UserScopedActor {

  import ws.kotonoha.server.util.KBsonDSL._

  def resolve(wr: String, rd: Option[String]): Unit = {
    val c = Candidate(wr, rd, None)
    val jv = c.toQuery
    val ji = JMDictRecord.findAll(jv, Limit(50))
    val tags = ji.headOption.toList.flatMap {
      r =>
        r.meaning.is.flatMap {
          m => m.info.is
        }.flatMap(e => JMDictInfo.process(e)).distinct
    }
    sender ! PossibleTags(tags)
  }

  def receive = {
    case PossibleTagRequest(wr, rd) => resolve(wr, rd)
  }
}
