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

import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.record.field.{DoubleField, IntField, StringField}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.lift.json.{JLCaseClass, JLRecord}
import ws.kotonoha.server.records.meta.KotonohaBsonMeta

/**
  * @author eiennohito
  * @since 2016/08/15
  */
class JsonRecordSpec extends FreeSpec with Matchers with PropertyChecks {
  implicit def trec = Arbitrary(Gen.resultOf { (s1: String, d1: Double, i3: Int) =>
    val rec = TestRec.createRecord
    rec.f1.set(s1)
    rec.f2.set(d1)
    rec.f3.set(i3)
    rec
  })

  "RecordTypeclasses" - {
    "work with a simple one" in {
      val wr = JLRecord.cnv[TestCC, TestRec]
      val ji = JLCaseClass.format[TestCC]
      forAll { r: TestRec =>
        val converted = wr.fromRecord(r)
        val fromJson = ji.read(r.asJValue)
        converted shouldBe fromJson
      }
    }
  }
}

private[json] class TestRec private() extends BsonRecord[TestRec] {
  object f1 extends StringField(this, 50)
  object f2 extends DoubleField(this)
  val f3 = new IntField(this) {
    override def name: String = "f3"
  }

  override def meta = TestRec
}

private[json] object TestRec extends TestRec with KotonohaBsonMeta[TestRec]

private[json] case class TestCC(f1: String, f3: Option[Int])
