package org.eiennohito.kotonoha.records

import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import org.eiennohito.kotonoha.model.EventTypes
import net.liftweb.mongodb.record.field.{LongRefField, LongPk}
import net.liftweb.record.field.{DateTimeField, DoubleField, IntField}

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

trait EventRecord[OwnerType <: MongoRecord[OwnerType]] extends LongPk[OwnerType] { self : OwnerType =>
  protected def myType: Int
  object eventType extends IntField(this.asInstanceOf[OwnerType], myType)
  object datetime extends DateTimeField(this.asInstanceOf[OwnerType])
}

class MarkEventRecord private() extends MongoRecord[MarkEventRecord] with LongPk[MarkEventRecord] with EventRecord[MarkEventRecord] {
  def meta = MarkEventRecord

  protected def myType = EventTypes.MARK

  object card extends LongRefField(this, WordCardRecord)
  object mode extends IntField(this)
  object mark extends DoubleField(this)
  object time extends DoubleField(this)
}

object MarkEventRecord extends MarkEventRecord with MongoMetaRecord[MarkEventRecord] with NamedDatabase