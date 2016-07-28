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

package ws.kotonoha.dict.jmdict

import java.util.regex.Pattern

import org.apache.lucene.search.BooleanClause.Occur

import scala.collection.mutable.ArrayBuffer

/**
  * @author eiennohito
  * @since 2016/07/28
  */

case class JmdictQueryPart(term: String, occur: Occur = Occur.SHOULD)

case class JmdictQuery(
  limit: Int,
  readings: Seq[JmdictQueryPart] = Nil,
  writings: Seq[JmdictQueryPart] = Nil,
  tags: Seq[JmdictQueryPart] = Nil,
  other: Seq[JmdictQueryPart] = Nil,
  ignore: Seq[Long] = Nil,
  explain: Boolean = false
)

object JmdictQuery {
  private val whitespace = Pattern.compile("\\s+")

  def fromString(qs: String, limit: Int = 50, explain: Boolean = false): JmdictQuery = {
    val parts = whitespace.split(qs).filter(_.length > 0)

    val rds = new ArrayBuffer[JmdictQueryPart]()
    val wrs = new ArrayBuffer[JmdictQueryPart]()
    val tags = new ArrayBuffer[JmdictQueryPart]()
    val other = new ArrayBuffer[JmdictQueryPart]()


    parts.foreach { p =>

      val (occur, next) = p.charAt(0) match {
        case '+' | '＋' => (Occur.MUST, p.substring(1))
        case '-' | '−' => (Occur.MUST_NOT, p.substring(1))
        case _ => (Occur.SHOULD, p)
      }

      next.split(":") match {
        case Array(pref, suf) =>
          val part = JmdictQueryPart(suf, occur)
          val coll = pref match {
            case s if s.startsWith("w") => wrs
            case s if s.startsWith("r") => rds
            case s if s.startsWith("t") => tags
            case _ => other
          }
          coll += part
        case _ =>
          other += JmdictQueryPart(next, occur)
      }
    }
    JmdictQuery(
      limit = limit,
      rds, wrs, tags, other,
      explain = explain
    )
  }
}

