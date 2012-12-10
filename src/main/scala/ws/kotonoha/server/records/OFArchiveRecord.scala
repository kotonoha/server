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

package ws.kotonoha.server.records

import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.mongodb.record.field.{ObjectIdField, MongoJsonObjectListField, ObjectIdPk, LongPk}
import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.record.field.{LongField, DateTimeField}
import net.liftweb.mongodb.{JsonObject, JsonObjectMeta}

/**
 * @author eiennohito
 * @since 27.10.12 
 */

case class OFElement(rep: Long, diff: Double, value: Double) extends JsonObject[OFElement] {
  def meta = OFElement
}
object OFElement extends JsonObjectMeta[OFElement]

class OFArchiveRecord private() extends MongoRecord[OFArchiveRecord] with ObjectIdPk[OFArchiveRecord] {
  def meta = OFArchiveRecord

  object timestamp extends DateTimeField(this) with DateJsonFormat
  object elems extends MongoJsonObjectListField[OFArchiveRecord, OFElement](this, OFElement)
  object user extends ObjectIdField(this)
  object matrix extends ObjectIdField(this)
}

object OFArchiveRecord extends OFArchiveRecord with MongoMetaRecord[OFArchiveRecord] with NamedDatabase
