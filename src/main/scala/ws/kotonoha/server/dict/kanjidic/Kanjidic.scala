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

package ws.kotonoha.server.dict.kanjidic

import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.server.records.dictionary.KanjidicRecord

/**
 * @author eiennohito
 * @since 14.03.13 
 */

object Kanjidic {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def inWord(s: String) = entries(UnicodeUtil.kanji(s))

  def entries(kanji: List[String]): Map[String, KanjidicRecord] = {
    val q = KanjidicRecord where (_.literal in kanji)
    val data = q fetch()
    data.map {
      i => i.literal.get -> i
    }.toMap
  }
}
