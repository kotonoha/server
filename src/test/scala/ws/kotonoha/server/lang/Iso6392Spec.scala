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

import org.scalatest.{FreeSpec, Matchers}

/**
  * @author eiennohito
  * @since 2017/02/16
  */
class Iso6392Spec extends FreeSpec with Matchers {
  "Iso6392" - {
    "supports several langauges" in {
      Iso6392.byBib("eng").englishNameAll shouldBe "English"
      Iso6392.byBib("fre").englishNameAll shouldBe "French"
      Iso6392.byBib("ger").englishNameAll shouldBe "German"
      Iso6392.byBib("rus").englishNameAll shouldBe "Russian"
    }
  }
}
