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

/**
 * @author eiennohito
 * @since 21.01.13 
 */

case class BasicTaggerTest(test: String => Boolean, tag: String)

class BasicTagger extends UserScopedActor {
  import UnicodeUtil._

  def T = BasicTaggerTest

  val tests = List[BasicTaggerTest](
    T(x => !UnicodeUtil.hasKanji(x), "no-kanji"),
    T(UnicodeUtil.isHiragana, "hira-only"),
    T(UnicodeUtil.isKatakana, "kata-only")
  )

  def handle(wr: String, rd: Option[String]): Unit = {
    val ret = tests.foldLeft(List[String]()) {
      case (lst, BasicTaggerTest(fnc, res)) => if (fnc(wr)) res :: lst else lst
    }

    sender ! PossibleTags(ret)
  }

  def receive = {
    case PossibleTagRequest(wr, rd) => handle(wr, rd)
  }
}
