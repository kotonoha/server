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

package ws.kotonoha.server.util

import org.scalatest.{FreeSpec, Matchers}

/**
  * @author eiennohito
  * @since 2017/02/16
  */
class LangUtilSpec extends FreeSpec with Matchers {
  "LangUtil" - {
    "correctly parses Accept-Language header content" in {
      val content = "en-US,en;q=0.8,ja;q=0.6,ru;q=0.4"
      val parsed = LangUtil.parseAcceptLanguage(content)
      parsed should have length (4)
      parsed.head.cultureCode shouldBe "en-US"
    }

    "correctly converts AcceptLanguage -> 3code" in {
      val content = "en-US,en;q=0.8,ja;q=0.6,ru;q=0.4"
      val parsed = LangUtil.parseAcceptLanguage(content)
      val codes = LangUtil.toLangCodes(parsed)
      codes shouldBe Seq("eng", "eng", "rus")
    }
  }

}
