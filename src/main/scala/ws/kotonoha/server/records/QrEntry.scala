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
import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field._

import ws.kotonoha.server.actors.LifetimeObjects
import net.liftweb.record.field.{LongField, TextareaField, BinaryField}

/**
 * @author eiennohito
 * @since 24.03.12
 */

trait Lifetime { self : MongoRecord[_] =>
  def record[T <: MongoRecord[T]] = self.asInstanceOf[MongoRecord[T]]
  def recid = self.id.asInstanceOf[ObjectIdField[_]].get
  def lifetimeObj: LifetimeObjects.LiftetimeObjects
}

trait UserRef {
  def user : ObjectIdRefField[_, UserRecord]
}

class QrEntry private() extends MongoRecord[QrEntry] with ObjectIdPk[QrEntry] with Lifetime with UserRef {
  def meta = QrEntry

  object user extends ObjectIdRefField(this, UserRecord)
  object binary extends BinaryField(this)
  object content extends TextareaField(this, 4000)

  def lifetimeObj = LifetimeObjects.QrEnt
}

object QrEntry extends QrEntry with MongoMetaRecord[QrEntry] with NamedDatabase
