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

package ws.kotonoha.dict.jmdict

import org.apache.lucene.util.BytesRefBuilder
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.PropertyChecks

/**
  * @author eiennohito
  * @since 2016/07/29
  */
class ValueConverterTest extends FreeSpec with PropertyChecks with Matchers {
  "value converter" - {
    "works" in {
      forAll { (inp: Long) =>
        val bb = new BytesRefBuilder
        DataConversion.writeSignedVLong(inp, bb)
        val lng = DataConversion.readSignedVLong(bb.toBytesRef)
        lng shouldBe inp
      }
    }
  }

}
