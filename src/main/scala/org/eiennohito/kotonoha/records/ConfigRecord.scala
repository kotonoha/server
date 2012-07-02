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

package org.eiennohito.kotonoha.records

import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field.LongPk
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.{OptionalBinaryField, OptionalStringField, StringField, BooleanField}

/**
 * @author eiennohito
 * @since 31.03.12
 */

class ConfigRecord private() extends MongoRecord[ConfigRecord] with LongPk[ConfigRecord] {

  override def defaultIdValue = 0L

  object inviteOnly extends BooleanField(this, false)
  object baseUri extends StringField(this, 250)
  object stokeUri extends StringField(this, 250, "")
  object myKey extends OptionalBinaryField(this)

  def hasStrokes = stokeUri.is.length == 0

  def meta = ConfigRecord
}

object ConfigRecord extends ConfigRecord with MongoMetaRecord[ConfigRecord] with NamedDatabase {
  private lazy val instance = synchronized { find(0L) openOr (createRecord) }
  def apply = instance
}

object AppConfig {
  def apply() = ConfigRecord.apply
  def save = apply().save
}
