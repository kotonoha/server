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

import net.liftweb.common.Box
import net.liftweb.json.JsonAST.{JBool, JInt, JString}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.lift.json.{JFormat, JWrite}

import scala.language.implicitConversions

/**
  * @author eiennohito
  * @since 2016/08/12
  */
class BasicSerializerSpec extends FreeSpec with Matchers with PropertyChecks with LiftBoxSupport {
  "LiftSerializer" - {
    "works with ints" in {
      forAll { (i: Int) =>
        val f = implicitly[JFormat[Int]]
        val jv = f.write(i)
        jv.asInstanceOf[JInt].num shouldBe i
        f.read(jv).open shouldBe i
      }
    }

    "works with longs" in {
      forAll { (i: Long) =>
        val f = implicitly[JFormat[Long]]
        val jv = f.write(i)
        jv.asInstanceOf[JInt].num shouldBe i
        f.read(jv).open shouldBe i
      }
    }


    "works with bools" in {
      forAll { (i: Boolean) =>
        val f = implicitly[JFormat[Boolean]]
        val jv = f.write(i)
        jv.asInstanceOf[JBool].value shouldBe i
        f.read(jv).open shouldBe i
      }
    }

    "works with strings" in {
      forAll { (i: String) =>
        val f = implicitly[JFormat[String]]
        val jv = f.write(i)
        jv.asInstanceOf[JString].s shouldBe i
        f.read(jv).open shouldBe i
      }
    }
  }
}

trait LiftBoxSupport {
  implicit def box2BoxOps[T](b: Box[T]): LiftBoxSupport.LiftBoxOps[T] = new LiftBoxSupport.LiftBoxOps[T](b)
}

object LiftBoxSupport {
  implicit class LiftBoxOps[T](val b: Box[T]) extends AnyVal {
    def open: T = b.openOrThrowException("it's ok")
  }
}
