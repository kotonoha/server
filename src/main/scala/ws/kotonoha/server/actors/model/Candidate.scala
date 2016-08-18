/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

import org.apache.lucene.search.BooleanClause.Occur
import ws.kotonoha.akane.dic.jmdict.{JMDictUtil, JmdictEntry}
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.dict.jmdict.{JmdictQuery, JmdictQueryPart}
import ws.kotonoha.lift.json.JLCaseClass
import ws.kotonoha.server.util.Strings
import ws.kotonoha.server.web.comet.InvalidStringException

/**
  * @author eiennohito
  * @since 2016/08/18
  */
case class Candidate(writing: String, reading: Option[String], meaning: Option[String]) {
  def query(limit: Int = 10): JmdictQuery = {
    JmdictQuery(
      limit = limit,
      writings = Seq(JmdictQueryPart(writing, Occur.SHOULD)),
      readings = reading.map(r => JmdictQueryPart(r, Occur.MUST)).toSeq,
      other = meaning.map(m => JmdictQueryPart(m, Occur.SHOULD)).toSeq
    )
  }

  def isOnlyKana = writing.length > 0 && UnicodeUtil.isKana(writing)

  def sameWR = reading match {
    case Some(`writing`) => true
    case _ => false
  }

  def sortResults(data: Seq[JmdictEntry], keep: Int = 3): Seq[JmdictEntry] = {
    def penalty(r: JmdictEntry) = {
      if ((isOnlyKana || sameWR) && r.writings.nonEmpty) 10 else 0
    }
    val processed = data.map(a => a -> (-JMDictUtil.calculatePriority(a) + penalty(a)))
    processed.sortBy(_._2).take(keep).map(_._1)
  }
}

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
        if (isKana(smt)) {
          new Candidate(w, wrap(smt), None)
        } else {
          new Candidate(w, None, wrap(smt))
        }
      }
      case _ => throw new InvalidStringException(in)
    }
  }

  implicit val candFmt = JLCaseClass.format[Candidate]
}
