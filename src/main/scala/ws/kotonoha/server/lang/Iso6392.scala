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

package ws.kotonoha.server.lang

import org.apache.commons.lang3.StringUtils
import ws.kotonoha.akane.resources.Classpath

/**
  *
  * Util for accessing language codes from ISO 639-2 Standard.
  *
  * @author eiennohito
  * @since 2017/02/16
  */
object Iso6392 {

  private def load() = {
    Classpath.lines("iso-639-2.txt").map { line =>
      StringUtils.splitPreserveAllTokens(line, '|') match {
        case Array(bib, term, a2, engName, frName) =>
          val engSplitted = StringUtils.splitPreserveAllTokens(engName, ';').map(_.trim)
          Iso6392LangEntry(bib, term, a2, engName, engSplitted, frName)
      }
    }.toVector
  }

  val definitions: Vector[Iso6392LangEntry] = load()

  lazy val byBib: Map[String, Iso6392LangEntry] = {
    definitions.map { d =>
      val key = if (d.bibliographic.length > 0) {
        d.bibliographic
      } else if (d.terminologic.length > 0) {
        d.terminologic
      } else d.alpha2
      key -> d
    }.toMap
  }

  def findByCode(iso3Code: String): Option[Iso6392LangEntry] = definitions.find(e => e.bibliographic == iso3Code || e.terminologic == iso3Code)
}

case class Iso6392LangEntry(
  bibliographic: String,
  terminologic: String,
  alpha2: String,
  englishNameAll: String,
  englishNames: IndexedSeq[String],
  frenchName: String
) {
  def englishName: String = englishNames.head
}
