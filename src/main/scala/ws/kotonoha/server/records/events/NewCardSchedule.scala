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

import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdPk}
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.record.field.StringField
import ws.kotonoha.server.records.meta.{JodaDateField, KotonohaMongoRecord}

/**
 * @author eiennohito
 * @since 27.02.13 
 */

class NewCardSchedule private() extends MongoRecord[NewCardSchedule] with ObjectIdPk[NewCardSchedule] {
  def meta = NewCardSchedule

  object user extends ObjectIdField(this)

  object card extends ObjectIdField(this)

  object tag extends StringField(this, 100)

  object date extends JodaDateField(this)

}

object NewCardSchedule extends NewCardSchedule with MongoMetaRecord[NewCardSchedule] with NamedDatabase with KotonohaMongoRecord[NewCardSchedule]
