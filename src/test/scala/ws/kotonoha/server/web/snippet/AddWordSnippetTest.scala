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

package ws.kotonoha.server.web.snippet

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{FunSuite, FreeSpec}

/**
 * @author eiennohito
 * @since 13.06.12
 */

class AddWordSnippetTest extends FunSuite with ShouldMatchers {
  test("matches from string with one") {
    val c = Candidate("help me!")
    c.writing should equal("help me!")
    c.reading should be (None)
    c.meaning should be (None)
  }

  test("matches from string with two params") {
      val c = Candidate("help me!|please!")
      c.writing should equal("help me!")
      c.reading should be (None)
      c.meaning should be (Some("please!"))
  }

  test("matches from string with kana") {
      val c = Candidate("help me!|ばか")
      c.writing should equal("help me!")
      c.reading should be (Some("ばか"))
      c.meaning should be (None)
  }
}
