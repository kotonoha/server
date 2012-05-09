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

package org.eiennohito.kotonoha.mongo

import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import net.liftweb.mongodb.record.field.LongPk
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import org.eiennohito.kotonoha.records.SequencedLongId
import net.liftweb.record.field.StringField
import org.eiennohito.kotonoha.mongodb.{MongoDbInit, NamedDatabase}

/**
 * @author eiennohito
 * @since 09.05.12
 */

class TestRecord2 private() extends MongoRecord[TestRecord2] with LongPk[TestRecord2] with SequencedLongId[TestRecord2] {
  def meta = TestRecord2

  object name extends StringField(this, 50)
}

object TestRecord2 extends TestRecord2 with MongoMetaRecord[TestRecord2] with NamedDatabase

class SequenceIdTest extends FreeSpec with ShouldMatchers {
  MongoDbInit.init()

  "SequenceId Test" - {
    "record saves in" in {
      val rec = TestRecord2.createRecord.name("test")
      rec.save
    }
  }

}
