package org.eiennohito.kotonoha.records

import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field._
import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord, MongoMetaRecord}
import net.liftweb.common.Empty
import net.liftweb.record.field.{DateTimeField, OptionalDateTimeField, IntField, StringField}
import org.eiennohito.kotonoha.utls.DateTimeUtils

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

class ExampleRecord private() extends BsonRecord[ExampleRecord] {
  def meta = ExampleRecord

  object example extends StringField(this, 250)
  object translation extends StringField(this, 500)
}

object ExampleRecord extends ExampleRecord with BsonMetaRecord[ExampleRecord]

class WordRecord private() extends MongoRecord[WordRecord] with LongPk[WordRecord] {
  def meta = WordRecord

  object writing extends StringField(this, 100)
  object reading extends StringField(this, 150)
  object meaning extends StringField(this, 1000)
  object createdOn extends DateTimeField(this)

  object examples extends BsonRecordListField(this, ExampleRecord)
  object user extends LongRefField(this, UserRecord)
}

object WordRecord extends WordRecord with MongoMetaRecord[WordRecord] with NamedDatabase

class WordCardRecord private() extends MongoRecord[WordCardRecord] with LongPk[WordCardRecord] {
  def meta = WordCardRecord

  object cardMode extends IntField(this)
  object word extends LongRefField(this, WordRecord)
  object learning extends BsonRecordField(this, ItemLearningDataRecord, Empty) {
    override def required_? = false
  }
  object user extends LongRefField(this, UserRecord)
  object createdOn extends DateTimeField(this)
  object notBefore extends OptionalDateTimeField(this, Empty)
}

object WordCardRecord extends WordCardRecord with MongoMetaRecord[WordCardRecord] with NamedDatabase