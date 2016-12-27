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

package ws.kotonoha.server.records.events

import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdPk}
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.IntField
import ws.kotonoha.examples.api.ExampleSentence
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.mongodb.record.PbufMessageField
import ws.kotonoha.server.records.meta.{JodaDateField, KotonohaMongoRecord}

/**
  * @author eiennohito
  * @since 2016/12/27
  */
class ExampleStatusReport extends MongoRecord[ExampleStatusReport] with ObjectIdPk[ExampleStatusReport] {
  import ws.kotonoha.server.examples.api.ApiLift._
  import ws.kotonoha.server.examples.ExamplesToBson._

  override def meta: MongoMetaRecord[ExampleStatusReport] = ExampleStatusReport

  object timestamp extends JodaDateField(this)
  object user extends ObjectIdField(this)
  object wordId extends ObjectIdField(this)
  object example extends PbufMessageField[ExampleStatusReport, ExampleSentence](this)
  object status extends IntField(this)
}

object ExampleStatusReport extends ExampleStatusReport with MongoMetaRecord[ExampleStatusReport]
  with KotonohaMongoRecord[ExampleStatusReport] with NamedDatabase
