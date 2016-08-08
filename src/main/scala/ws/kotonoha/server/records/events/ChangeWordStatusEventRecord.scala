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

import net.liftweb.mongodb.record.field.{ObjectIdPk, ObjectIdRefField}
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import ws.kotonoha.model.{EventTypes, WordStatus}
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.records.meta.{KotonohaMongoRecord, PbEnumField}

class ChangeWordStatusEventRecord private() extends MongoRecord[ChangeWordStatusEventRecord]
with ObjectIdPk[ChangeWordStatusEventRecord] with EventRecord[ChangeWordStatusEventRecord] {
  def meta = ChangeWordStatusEventRecord

  protected def myType = EventTypes.ChangeWordStatus

  object word extends ObjectIdRefField(this, WordRecord)

  object toStatus extends PbEnumField(this, WordStatus)
}

object ChangeWordStatusEventRecord extends ChangeWordStatusEventRecord
with MongoMetaRecord[ChangeWordStatusEventRecord] with NamedDatabase
with KotonohaMongoRecord[ChangeWordStatusEventRecord]
