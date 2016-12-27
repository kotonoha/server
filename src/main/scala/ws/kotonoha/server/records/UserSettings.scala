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

import net.liftweb.mongodb.record.field.{MongoListField, ObjectIdPk}
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.{BooleanField, IntField}
import org.bson.types.ObjectId
import ws.kotonoha.server.ioc.UserContextService
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.util.KotoGlobal

/**
 * @author eiennohito
 * @since 06.07.12
 */

class UserSettings private() extends MongoRecord[UserSettings] with ObjectIdPk[UserSettings] {
  def meta = UserSettings

  object badCount extends IntField(this, 20)

  object lastTags extends MongoListField[UserSettings, String](this)

  object stalePriorities extends BooleanField(this, false)
}

object UserSettings extends UserSettings with MongoMetaRecord[UserSettings] with NamedDatabase {
  def current: UserSettings = {
    val users = KotoGlobal.container.inst[UserContextService]
    users.of(UserRecord.currentId.openOrThrowException("user was not present")).settings
  }

  def forUser(id: ObjectId): UserSettings = find(id).openOr(UserSettings.createRecord.id(id).save())
}

