/*
 * Copyright 2012-2013 eiennohito
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

package ws.kotonoha.server.records.misc

import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.mongodb.record.field.{MongoCaseClassListField, MongoCaseClassField, ObjectIdField, ObjectIdPk}
import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.record.field.{StringField, LongField}
import ws.kotonoha.server.actors.schedulers.{State, ReviewCard, CardRequest}
import ws.kotonoha.server.records.{KotonohaMongoRecord, JodaDateField}
import net.liftweb.json.DefaultFormats
import ws.kotonoha.server.util.{EnumToStringSerializer, OidSerializer}

/**
 * @author eiennohito
 * @since 09.04.13 
 */

class ScheduleRecord private() extends MongoRecord[ScheduleRecord] with ObjectIdPk[ScheduleRecord] {
  def meta = ScheduleRecord

  private val format = DefaultFormats ++
    Seq(OidSerializer, EnumToStringSerializer.instance(State))

  object req extends MongoCaseClassField[ScheduleRecord, CardRequest](this) {
    override def formats = format
  }
  object cards extends MongoCaseClassListField[ScheduleRecord, ReviewCard](this) {
    override def formats = format
  }
  object user extends ObjectIdField(this)
  object date extends JodaDateField(this)
}

object ScheduleRecord extends ScheduleRecord with MongoMetaRecord[ScheduleRecord] with NamedDatabase with KotonohaMongoRecord[ScheduleRecord]

class CardSchedule private() extends MongoRecord[CardSchedule] with ObjectIdPk[CardSchedule] {
  def meta = CardSchedule

  object seq extends LongField(this)
  object source extends StringField(this, 100)
  object card extends ObjectIdField(this)
  object date extends JodaDateField(this)
  object user extends ObjectIdField(this)
  object bundle extends ObjectIdField(this)
}

object CardSchedule extends CardSchedule with MongoMetaRecord[CardSchedule] with NamedDatabase with KotonohaMongoRecord[CardSchedule]
