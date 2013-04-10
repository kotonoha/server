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

package ws.kotonoha.server.records.events

import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdRefField, ObjectIdPk}
import ws.kotonoha.server.model.EventTypes
import net.liftweb.record.field.{LongField, StringField, DoubleField, IntField}
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.records.{KotonohaMongoRecord, WordCardRecord}

/**
 * @author eiennohito
 * @since 18.01.13 
 */

class MarkEventRecord private() extends MongoRecord[MarkEventRecord] with ObjectIdPk[MarkEventRecord] with EventRecord[MarkEventRecord] {
  def meta = MarkEventRecord

  protected def myType = EventTypes.MARK

  object card extends ObjectIdRefField(this, WordCardRecord)

  object mode extends IntField(this)

  object mark extends DoubleField(this)

  object time extends DoubleField(this)

  object diff extends DoubleField(this)

  //my difficulty when learning
  object interval extends DoubleField(this)

  object lapse extends IntField(this)

  object rep extends IntField(this)

  object source extends StringField(this, 100)

  object seq extends LongField(this)

  object bundle extends ObjectIdField(this)

}

object MarkEventRecord extends MarkEventRecord with KotonohaMongoRecord[MarkEventRecord] with MongoMetaRecord[MarkEventRecord] with NamedDatabase
