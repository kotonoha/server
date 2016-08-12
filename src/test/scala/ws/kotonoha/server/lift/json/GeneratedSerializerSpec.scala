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

package ws.kotonoha.server.lift.json

import net.liftweb.json.{DefaultFormats, Extraction}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.lift.json.JLCaseClass


/**
  * @author eiennohito
  * @since 2016/08/12
  */

private[json] case class Test1(i: Int, l: Long, s: String, io: Option[Int])

private [json] object Test1 {

}


class GeneratedSerializerSpec extends FreeSpec with Matchers with PropertyChecks {
  val wr = JLCaseClass.format[Test1]

  "GeneratesSerializer" - {

    "works with case class" in {
      val msg = Test1(3, 2, "asd", Some(3))
      val jv1 = wr.write(msg)
      implicit val fmts = DefaultFormats
      val jv2 = Extraction.decompose(msg)
      val o1 = wr.read(jv2).openOrThrowException("ok")
      val o2 = Extraction.extract[Test1](jv1)
      o1 shouldBe o2
      msg shouldBe o1
      msg shouldBe o2
    }

    "works with case class and properies" in {
      forAll { (i1: Int, l1: Long, s1: String, io: Option[Int]) =>
        val msg = Test1(i1, l1, s1, io)
        val jv1 = wr.write(msg)
        implicit val fmts = DefaultFormats
        val jv2 = Extraction.decompose(msg)
        val o1 = wr.read(jv2).openOrThrowException("ok")
        val o2 = Extraction.extract[Test1](jv1)
        o1 shouldBe o2
        msg shouldBe o1
        msg shouldBe o2
      }
    }
  }
}
