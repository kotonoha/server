package org.eiennohito.kotonoha.records

import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.record.field.{IntField, DoubleField}
import net.liftweb.mongodb.record.field.{LongRefField, DateField, LongPk}

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
 * @since 29.01.12
 */


class ItemLearningDataRecord private() extends MongoRecord[ItemLearningDataRecord] with LongPk[ItemLearningDataRecord] {
  def meta = ItemLearningDataRecord

  object intervalStart extends DateField(this)
  object intervalEnd extends DateField(this)
  object intervalLength extends DoubleField(this)

  object difficulty extends DoubleField(this)
  object lapse extends IntField(this)
  object repetition extends IntField(this)
}

object ItemLearningDataRecord extends ItemLearningDataRecord with MongoMetaRecord[ItemLearningDataRecord] with NamedDatabase

class OFMatrixRecord private() extends MongoRecord[OFMatrixRecord] with LongPk[OFMatrixRecord] {
  def meta = OFMatrixRecord

  object refUser extends LongRefField(this, UserRecord)
}

object OFMatrixRecord extends OFMatrixRecord with MongoMetaRecord[OFMatrixRecord] with NamedDatabase

class OFElementRecord private() extends MongoRecord[OFElementRecord] with LongPk[OFElementRecord] {
  def meta = OFElementRecord

  object n extends IntField(this)
  object ef extends DoubleField(this)
  object value extends DoubleField(this)
}

object OFElementRecord extends OFElementRecord with MongoMetaRecord[OFElementRecord] with NamedDatabase
