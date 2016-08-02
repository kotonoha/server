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

package ws.kotonoha.server.mongo

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.server.mongodb.{MongoDbInit, NamedDatabase}
import net.liftweb.mongodb.record.field.LongPk
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.EnumField
import ws.kotonoha.server.records.{WordRecord, WordStatus}

/**
 * @author eiennohito
 * @since 16.05.12
 */

object TestEnum extends Enumeration {
  val First, Second = Value
}

class EnumTestRec private() extends MongoRecord[EnumTestRec] with LongPk[EnumTestRec] {
  def meta = EnumTestRec

  object fld extends EnumField(this, TestEnum)
}

object EnumTestRec extends EnumTestRec with MongoMetaRecord[EnumTestRec] with NamedDatabase

class EnumFieldTest extends FreeSpec with Matchers with MongoAwareTest {
  MongoDbInit.init()

  "Enum field test" - {
    "successfully saves" in {
      val rec = EnumTestRec.createRecord
      rec.fld(TestEnum.Second).save
    }

    "save bad word and print its id" in {
      val word = WordRecord.createRecord
      word.status(WordStatus.ReviewWord)
      word.save
      println("word id is " + word.id.get)
    }
  }
}
