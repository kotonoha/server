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

import net.liftweb.json.DefaultFormats
import net.liftweb.mongodb.record.field.{MongoCaseClassListField, ObjectIdField, ObjectIdPk}
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.{BooleanField, OptionalStringField, StringField}
import ws.kotonoha.model.EventTypes
import ws.kotonoha.server.actors.tags.{TagOp, TagOps}
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.records.meta.KotonohaMongoRecord

/**
 * @author eiennohito
 * @since 18.01.13 
 */

class AddWordRecord private() extends MongoRecord[AddWordRecord] with ObjectIdPk[AddWordRecord] with EventRecord[AddWordRecord] {
  def meta = AddWordRecord

  protected def myType = EventTypes.Add

  object processed extends BooleanField(this, false)

  object writing extends StringField(this, 100)

  object reading extends OptionalStringField(this, 100)

  object meaning extends OptionalStringField(this, 500)

  object group extends ObjectIdField(this)

  object tags extends MongoCaseClassListField[AddWordRecord, TagOp](this) {
    override def formats = DefaultFormats + TagOps
  }

  object source extends StringField(this, 100)
}

object AddWordRecord extends AddWordRecord with MongoMetaRecord[AddWordRecord] with NamedDatabase
with KotonohaMongoRecord[AddWordRecord]

