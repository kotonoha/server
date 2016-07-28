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

package ws.kotonoha.server.actors.model

import akka.util.Timeout
import ws.kotonoha.server.actors.dict.{DictionaryEntry, SearchResult}
import ws.kotonoha.server.actors.schedulers.AkkaWithUser

/**
 * @author eiennohito
 * @since 22.10.12 
 */

class WordCreateActorTest extends AkkaWithUser {
  import concurrent.duration._

  implicit val to: Timeout = 10 seconds

  "WordCreateActorTest" - {
    "makes katakana-only to have hiragana readings and katakana writings" in {
      val wca = usvc.userActor[WordCreateActor]("wordcreate")
      val ac = wca.underlyingActor
      val sr = SearchResult(
        DictionaryEntry(Nil, "アメリカ" :: Nil, Nil ) :: Nil
      )
      val data = ac.collapse(sr, "")
      data.data should have length (1)
      data.data(0) should have (
        'writing ("アメリカ"),
        'reading ("あめりか")
      )
    }
  }
}
