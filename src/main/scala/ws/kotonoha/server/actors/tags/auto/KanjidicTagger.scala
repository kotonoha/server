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
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.server.records.dictionary.{KanjidicRecord}
import ws.kotonoha.server.dict.kanjidic.Kanjidic

/**
 * @author eiennohito
 * @since 20.01.13 
 */

class KanjidicTagger extends UserScopedActor {

  def checkSingleKun(wr: String, rd: String, kanji: List[String],
                     entr: Map[String, KanjidicRecord]): List[String] = {
    if (kanji.length != 1) {
      Nil
    } else {
      val yomi = entr.get(kanji.head).toList.flatMap(_.rmgroups.is.flatMap(_.cleanKunyomi))
      yomi.contains(rd) match {
        case true => List("lonely-kun")
        case false => Nil
      }
    }
  }

  def checkOnlyOn(wr: String, rd: String, kanji: List[String],
                  entr: Map[String, KanjidicRecord]): List[String] = {
    Nil
  }

  def handle(wr: String, rd: String): Unit = {
    val kanji = UnicodeUtil.kanji(wr)
    val entrs = Kanjidic.entries(kanji)
    sender ! PossibleTags(
      checkSingleKun(wr, rd, kanji, entrs) ++
        checkOnlyOn(wr, rd, kanji, entrs)
    )
  }

  def receive = {
    case PossibleTagRequest(wr, r) => {
      r match {
        case Some(rd) => handle(wr, rd)
        case None => sender ! PossibleTags(Nil)
      }
    }
  }
}
