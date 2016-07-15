/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.records.{WordRecord, KotonohaMongoRecord}
import ws.kotonoha.server.model.EventTypes
import net.liftweb.mongodb.record.field.{MongoCaseClassListField, MongoListField, ObjectIdRefField, MongoRefField}
import ws.kotonoha.server.actors.tags.{TagOps, TagOp}
import net.liftweb.json.DefaultFormats

/**
 * @author eiennohito
 * @since 18.01.13 
 */

class TagEvent private() extends MongoRecord[TagEvent] with EventRecord[TagEvent] {
  def meta = TagEvent

  protected def myType = EventTypes.TAG

  object word extends ObjectIdRefField(this, WordRecord)

  object tags extends MongoListField[TagEvent, String](this)

  object ops extends MongoCaseClassListField[TagEvent, TagOp](this) {
    override def formats = DefaultFormats + TagOps
  }

}

object TagEvent extends TagEvent with MongoMetaRecord[TagEvent] with KotonohaMongoRecord[TagEvent] with NamedDatabase
