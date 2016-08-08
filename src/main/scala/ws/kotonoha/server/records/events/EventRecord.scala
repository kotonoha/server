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

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.field._
import ws.kotonoha.model.EventTypes
import ws.kotonoha.server.records.UserRecord
import ws.kotonoha.server.records.meta.{JodaDateField, PbEnumField}

/**
 * @author eiennohito
 * @since 30.01.12
 */

trait EventRecord[OwnerType <: MongoRecord[OwnerType]] extends ObjectIdPk[OwnerType] { self: OwnerType =>
  protected def myType: EventTypes

  object eventType extends PbEnumField(this.asInstanceOf[OwnerType], EventTypes, myType.value)

  object datetime extends JodaDateField(this.asInstanceOf[OwnerType])

  object user extends ObjectIdRefField(this.asInstanceOf[OwnerType], UserRecord)
}
