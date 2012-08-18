package ws.kotonoha.server.kanji

import java.io.InputStreamReader
import collection.mutable.HashMap
import ws.kotonoha.akane.unicode.UnicodeUtil


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
/**
 * @author eiennohito
 * @since 02.03.12
 */

case class KanjiEntry(kanji: String, count: Int, category: KanjiType.KanjiType)

class KanjiTagResult(val oldKanji: List[KanjiEntry],
                      val newKanji: List[KanjiEntry],
                      val removedKanji: List[KanjiEntry],
                      val absentKanji: List[KanjiEntry]) {
  def total = oldKanji ++ newKanji ++ removedKanji ++ absentKanji
}

class KanjiTagger {
  def tag(r: InputStreamReader) = {
    val kanji = UnicodeUtil.stream(r).filter(UnicodeUtil.isKanji(_)).foldLeft(new HashMap[String, Int]()) {
      case (map, el) => {
        val c = new String(Character.toChars(el))
        map.get(c) match {
          case Some(v) => map.put(c, v + 1)
          case None => map.put(c, 1)
        }
        map
      }
    }

    val cats = kanji.map {
      case (k, cnt) => KanjiEntry(k, cnt, Jouyou.category(k))
    }.groupBy(_.category)

    import KanjiType._

    new KanjiTagResult(
      cats.get(Old).map(_.toList) getOrElse Nil,
      cats.get(New).map(_.toList) getOrElse Nil,
      cats.get(Removed).map(_.toList) getOrElse Nil,
      cats.get(Absent).map(_.toList) getOrElse Nil
    )
  }
}
