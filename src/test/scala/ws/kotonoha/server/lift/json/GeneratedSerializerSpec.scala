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
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.lift.json.{JFormat, JLCaseClass}


/**
  * @author eiennohito
  * @since 2016/08/12
  */

private[json] case class Test1(i: Int, l: Long, s: String, io: Option[Int])
private[json] case class Test2(t1: Test1, t2: Option[Test1], t3: List[Test1])
private[json] case class Test3(t1: Seq[Test1])

class GeneratedSerializerSpec extends FreeSpec with Matchers with PropertyChecks {
  implicit val fmt1 = JLCaseClass.format[Test1]
  implicit val fmt2 = JLCaseClass.format[Test2]
  implicit val fmt3 = JLCaseClass.format[Test3]

  implicit val t1a: Arbitrary[Test1] = Arbitrary(Gen.resultOf(Test1))
  implicit val t2a: Arbitrary[Test2] = Arbitrary(Gen.resultOf(Test2))
  implicit val t3a: Arbitrary[Test3] = Arbitrary(Gen.resultOf(Test3))

  def checkEquiv[T](msg: T)(implicit fmt: JFormat[T], mf: Manifest[T]): Unit = {
    val jv1 = fmt.write(msg)
    implicit val fmts = DefaultFormats
    val jv2 = Extraction.decompose(msg)
    val o1 = fmt.read(jv2).openOrThrowException("ok")
    val o2 = Extraction.extract[T](jv1)
    o1 shouldBe o2
    msg shouldBe o1
    msg shouldBe o2
  }

  "GeneratesSerializer" - {
    "works with case class" in {
      forAll { (msg: Test1) => checkEquiv(msg) }
    }

    "works with two classes" in {
      forAll { (msg: Test2) => checkEquiv(msg) }
    }

    "works with three classes" in {
      forAll { (msg: Test3) => checkEquiv(msg) }
    }
  }
}
