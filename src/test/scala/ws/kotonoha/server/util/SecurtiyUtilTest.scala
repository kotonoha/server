/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}


class SecurtiyUtilTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {
  "SecurityUtil" - {
    "encodes and decodes text with AES" in {
      forAll { plaintext: String =>
        val key = SecurityUtil.randomBytes(16)
        val cyphertext = SecurityUtil.encryptAes(plaintext, key)
        val decrypted = SecurityUtil.decryptAes(cyphertext, key)
        decrypted shouldBe plaintext
      }
    }
  }
}
