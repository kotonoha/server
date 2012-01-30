package org.eiennohito.kotonoha.records

import net.liftweb.mongodb.record.field.{LongRefListField, LongRefField, LongPk}
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.StringField
import org.eiennohito.kotonoha.mongodb.NamedDatabase

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

/**
 * @author eiennohito
 * @since 30.01.12
 */

class ExampleRecord private() extends MongoRecord[ExampleRecord] with LongPk[ExampleRecord] {
  def meta = ExampleRecord

  object example extends StringField(this, 250)
  object translation extends StringField(this, 500)
}

object ExampleRecord extends ExampleRecord with MongoMetaRecord[ExampleRecord] with NamedDatabase

class WordRecord private() extends MongoRecord[WordRecord] with LongPk[WordRecord] {
  def meta = WordRecord

  object writing extends StringField(this, 100)
  object reading extends StringField(this, 150)
  object meaning extends StringField(this, 1000)

  object examples extends LongRefListField(this, ExampleRecord)
  object learning extends LongRefField(this, ItemLearningDataRecord)
  object refUser extends LongRefField(this, UserRecord)

}

object WordRecord extends WordRecord with MongoMetaRecord[WordRecord] with NamedDatabase