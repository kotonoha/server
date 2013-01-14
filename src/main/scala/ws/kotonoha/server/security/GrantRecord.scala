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

package ws.kotonoha.server.security

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.mongodb.record.field.{ObjectIdField, LongPk}
import net.liftweb.record.field.{StringField, LongField}
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import com.mongodb.WriteConcern

/**
 * @author eiennohito
 * @since 18.08.12
 */

class GrantRecord private() extends MongoRecord[GrantRecord] with LongPk[GrantRecord] {
  def meta = GrantRecord

  object user extends ObjectIdField(this)
  object role extends StringField(this, 50)
}

object GrantRecord extends GrantRecord with MongoMetaRecord[GrantRecord] with NamedDatabase {
  import com.foursquare.rogue.LiftRogue._
  def revokeRole(user: ObjectId, role: String): Unit = {
    GrantRecord where (_.user eqs (user)) and (_.role eqs (role)) bulkDelete_!!(WriteConcern.NORMAL)
  }

  def haveGrant(user: ObjectId, role: String) = {
    val cnt = GrantRecord where (_.user eqs (user)) and (_.role eqs (role)) count()
    cnt != 0
  }
}
