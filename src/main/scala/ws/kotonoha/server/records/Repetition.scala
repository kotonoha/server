package ws.kotonoha.server.records

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field.{ObjectIdPk, ObjectIdRefField}
import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.{IntField, DoubleField}
import org.bson.types.ObjectId

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

import ws.kotonoha.server.mongodb.KotonohaLiftRogue._


class ItemLearningDataRecord private() extends BsonRecord[ItemLearningDataRecord] {
  def meta = ItemLearningDataRecord

  object intervalStart extends JodaDateField(this)
  object intervalEnd extends JodaDateField(this)
  object intervalLength extends DoubleField(this)

  object difficulty extends DoubleField(this, 2.5)
  object inertia extends DoubleField(this, 1.0)
  object lapse extends IntField(this)
  object repetition extends IntField(this)
}

object ItemLearningDataRecord extends ItemLearningDataRecord with KotonohaMongoRecord[ItemLearningDataRecord] with BsonMetaRecord[ItemLearningDataRecord]

class OFMatrixRecord private() extends MongoRecord[OFMatrixRecord] with ObjectIdPk[OFMatrixRecord] with UserRef {
  def meta = OFMatrixRecord
  object user extends ObjectIdRefField(this, UserRecord)
}

object OFMatrixRecord extends OFMatrixRecord with MongoMetaRecord[OFMatrixRecord] with NamedDatabase {
  def forUser(userId: ObjectId) =  {
    val m = OFMatrixRecord where (_.user eqs userId) get()
    m match {
      case Some(mat) => mat
      case None => {
        val mat = OFMatrixRecord.createRecord
        mat.user(userId)
        mat.save
        mat
      }        
    }
  }
}

class OFElementRecord private() extends MongoRecord[OFElementRecord] with ObjectIdPk[OFElementRecord] {
  def meta = OFElementRecord

  object n extends IntField(this)
  object ef extends DoubleField(this)
  object value extends DoubleField(this)
  
  object matrix extends ObjectIdRefField(this, OFMatrixRecord)
}

object OFElementRecord extends OFElementRecord with MongoMetaRecord[OFElementRecord] with NamedDatabase {  
}
