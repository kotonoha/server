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

package ws.kotonoha.server.mongodb

import net.liftweb.mongodb.record.field.LongPk
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import org.scalatest.{FreeSpec, LoneElement, Matchers}
import ws.kotonoha.model.WordStatus
import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.records.meta.{KotonohaMongoRecord, PbEnumField}

/**
 * @author eiennohito
 * @since 16.05.12
 */


class EnumTestRec private() extends MongoRecord[EnumTestRec] with LongPk[EnumTestRec] {
  def meta = EnumTestRec

  object fld extends PbEnumField(this, WordStatus, WordStatus.New)
}

object EnumTestRec extends EnumTestRec with MongoMetaRecord[EnumTestRec] with KotonohaMongoRecord[EnumTestRec] with NamedDatabase

class EnumFieldTest extends FreeSpec with Matchers with MongoAwareTest with LoneElement {
  "Enum field test" - {
    "successfully saves" in {
      val rec = EnumTestRec.createRecord
      val id = 51231L
      rec.id(id).fld(WordStatus.Deleting).save()
      val saved = EnumTestRec.find(id)
      saved.openOrThrowException("exists").fld.get shouldBe WordStatus.Deleting
    }

    "save bad word" in {
      import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

      val word = WordRecord.createRecord
      word.status(WordStatus.ReviewWord)
      word.save()
      val query = WordRecord where (_.id eqs word.id.get) and (_.status eqs WordStatus.ReviewWord)
      val loaded = query.fetch()
      loaded.loneElement.status.get shouldBe WordStatus.ReviewWord
    }
  }
}
