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

package ws.kotonoha.server.dict

import java.nio.ByteBuffer
import java.io.File


class TatoebaLinkTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  test("test it works") {
    val bb = ByteBuffer.allocate(1024)
    val link = TatoebaLink(5, 6, "eng", "rus")
    link.toBuffer(bb)
    bb.rewind()
    val l2 = TatoebaLink.fromBuffer(bb)
    l2 should equal (link)
  }

  test("finder works") {
    val fnd = new TatoebaLinks(new File("e:\\Temp\\wap_soft\\tatoeba\\links.bin"))
    val vals = fnd.from(1089200).toList
    val i = 0
  }
}
