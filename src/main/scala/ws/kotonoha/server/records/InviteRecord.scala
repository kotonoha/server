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

package ws.kotonoha.server.records

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field.{ObjectIdPk, LongPk}
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.StringField

/**
 * @author eiennohito
 * @since 21.02.12
 */

class InviteRecord private() extends MongoRecord[InviteRecord] with ObjectIdPk[InviteRecord] {
  def meta = InviteRecord

  object key extends StringField(this, 32)
}

object InviteRecord extends InviteRecord with MongoMetaRecord[InviteRecord] with NamedDatabase
