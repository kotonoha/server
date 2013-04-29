/*
 * Copyright 2012-2013 eiennohito
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
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdPk}
import net.liftweb.record.field.{LongField, StringField}

/**
 * @author eiennohito
 * @since 16.04.13 
 */

class WikiPageRecord private() extends MongoRecord[WikiPageRecord] with ObjectIdPk[WikiPageRecord] {
  def meta = WikiPageRecord

  object path extends StringField(this, 255)
  object parent extends ObjectIdField(this) {
    override def required_? = false
  }
  object editor extends ObjectIdField(this)
  object datetime extends JodaDateField(this)
  object size extends LongField(this, 0)

  object comment extends StringField(this, 1024)
  object source extends StringField(this, 1024 * 1024) //1m
}

object WikiPageRecord extends WikiPageRecord with MongoMetaRecord[WikiPageRecord] with NamedDatabase with KotonohaMongoRecord[WikiPageRecord]
