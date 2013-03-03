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

import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdPk, LongPk}
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.actors.LifetimeObjects
import net.liftweb.record.field.{DateTimeField, EnumField, LongField}

/**
 * @author eiennohito
 * @since 24.03.12
 */

class LifetimeObj private() extends MongoRecord[LifetimeObj] with ObjectIdPk[LifetimeObj] {
  def meta = LifetimeObj

  object obj extends ObjectIdField(this)

  object objtype extends EnumField(this, LifetimeObjects)

  object deadline extends JodaDateField(this)

}

object LifetimeObj extends LifetimeObj with MongoMetaRecord[LifetimeObj] with NamedDatabase with KotonohaMongoRecord[LifetimeObj]
