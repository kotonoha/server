/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdPk}
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.records.meta.KotonohaMongoRecord

/**
  * @author eiennohito
  * @since 2016/08/12
  */

class ExpectedRepetition private() extends MongoRecord[ExpectedRepetition] with ObjectIdPk[ExpectedRepetition] {
  override def meta = ExpectedRepetition

  object user extends ObjectIdField(this)
  object word extends ObjectIdField(this)
  object card extends ObjectIdField(this)

}

object ExpectedRepetition extends ExpectedRepetition with KotonohaMongoRecord[ExpectedRepetition] with NamedDatabase
