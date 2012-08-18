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

package ws.kotonoha.server.records.dictionary

import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.mongodb.record.field.{MongoListField, LongPk}
import net.liftweb.record.field.{LongField, StringField}
import ws.kotonoha.server.mongodb.{DictDatabase, NamedDatabase}

/**
 * @author eiennohito
 * @since 19.04.12
 */

class ExampleSentenceRecord private() extends MongoRecord[ExampleSentenceRecord] with LongPk[ExampleSentenceRecord] {
  def meta = ExampleSentenceRecord

  object lang extends StringField(this, 3)
  object content extends StringField(this, 500)
  object tags extends MongoListField[ExampleSentenceRecord, String](this)
}

object ExampleSentenceRecord extends ExampleSentenceRecord with MongoMetaRecord[ExampleSentenceRecord] with DictDatabase {
  override def collectionName = "ex.sentences"
  import com.foursquare.rogue.Rogue._

  def langOf(id: Long): String = {
    val lang = ExampleSentenceRecord where (_.id eqs id) select(_.lang) get()
    lang.getOrElse("non")
  }
}

class ExampleLinkRecord private() extends MongoRecord[ExampleLinkRecord] with LongPk[ExampleLinkRecord] {
  def meta = ExampleLinkRecord

  object left extends LongField(this)
  object leftLang extends StringField(this, 3)
  object right extends LongField(this)
  object rightLang extends StringField(this, 3)
}

object ExampleLinkRecord extends ExampleLinkRecord with MongoMetaRecord[ExampleLinkRecord] with DictDatabase {
  override def collectionName = "ex.links"
}
