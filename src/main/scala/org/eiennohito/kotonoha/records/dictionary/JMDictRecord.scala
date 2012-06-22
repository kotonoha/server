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

package org.eiennohito.kotonoha.records.dictionary

import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.{StringField, EnumField}
import net.liftweb.mongodb.record.field.{MongoListField, MongoCaseClassListField, BsonRecordListField, LongPk}
import org.eiennohito.kotonoha.mongodb.DictDatabase
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json._
import net.liftweb.mongodb.Limit

/**
 * @author eiennohito
 * @since 14.04.12
 */

case class LocString(str: String, loc: String)

class JMDictMeaning extends BsonRecord[JMDictMeaning] {
  def meta = JMDictMeaning

  object info extends MongoListField[JMDictMeaning, String](this)
  object vals extends MongoCaseClassListField[JMDictMeaning, LocString](this)
}

object JMDictMeaning extends JMDictMeaning with BsonMetaRecord[JMDictMeaning]

case class Priority(value: String)

class JMString extends BsonRecord[JMString] {
  def meta = JMString

  object priority extends MongoCaseClassListField[JMString, Priority](this)
  object value extends StringField(this, 500)
}

object JMString extends JMString with BsonMetaRecord[JMString]

class JMDictRecord private() extends MongoRecord[JMDictRecord] with LongPk[JMDictRecord] {
  def meta = JMDictRecord

  object reading extends BsonRecordListField(this, JMString)
  object writing extends BsonRecordListField(this, JMString)
  object meaning extends BsonRecordListField(this, JMDictMeaning)
}

object JMDictRecord extends JMDictRecord with MongoMetaRecord[JMDictRecord] with DictDatabase {
  def query(q: String, max: Int) = {
    import org.eiennohito.kotonoha.util.KBsonDSL._
    val re = "^" + q
    val inner = "$regex" -> re
    val query: JObject = "$or" -> List(("writing.value" -> inner), ("reading.value" -> inner))
    val jse = compact(render(query))
    JMDictRecord.findAll(query, Limit(max))
  }
}
