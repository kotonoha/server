package ws.kotonoha.server.records.events

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import ws.kotonoha.server.model.EventTypes
import net.liftweb.record.field._
import net.liftweb.mongodb.record.field._
import ws.kotonoha.server.records.{UserRecord, JodaDateField}

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

trait EventRecord[OwnerType <: MongoRecord[OwnerType]] extends ObjectIdPk[OwnerType] {
  self: OwnerType =>
  protected def myType: Int

  object eventType extends IntField(this.asInstanceOf[OwnerType], myType)

  object datetime extends JodaDateField(this.asInstanceOf[OwnerType])

  object user extends ObjectIdRefField(this.asInstanceOf[OwnerType], UserRecord)

}
