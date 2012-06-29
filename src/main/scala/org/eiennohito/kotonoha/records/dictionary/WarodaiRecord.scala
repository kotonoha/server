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

import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.mongodb.record.field.{MongoListField, LongPk}
import net.liftweb.record.field.{StringField, IntField}
import org.eiennohito.kotonoha.mongodb.DictDatabase
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json._
import net.liftweb.mongodb.Limit

/**
 * @author eiennohito
 * @since 12.04.12
 */

class WarodaiRecord private() extends MongoRecord[WarodaiRecord] with LongPk[WarodaiRecord] {
  def meta = WarodaiRecord

  object posVol extends IntField(this)
  object posPage extends IntField(this)
  object posNum extends IntField(this)

  object readings extends MongoListField[WarodaiRecord, String](this)
  object writings extends MongoListField[WarodaiRecord, String](this)
  object rusReadings extends MongoListField[WarodaiRecord, String](this)

  object body extends StringField(this, 8192)
}

object WarodaiRecord extends WarodaiRecord with MongoMetaRecord[WarodaiRecord] with DictDatabase {
  def query(w: String, rd: Option[String], max: Int) = {
    import org.eiennohito.kotonoha.util.KBsonDSL._

    val query: JObject = rd match {
      case Some(r) => ("readings" -> bre(r)) ~ ("writings" -> bre(w))
      case None => "$or" -> List(("readings" -> bre(w)), ("writings" -> bre(w)))
    }

    findAll(query, Limit(max))
  }
}
