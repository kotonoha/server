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

import net.liftweb.json.JsonAST.{JBool, JField, JObject, _}
import net.liftweb.mongodb.record.field._
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.{IntField, OptionalLongField, StringField}
import ws.kotonoha.examples.api.ExamplePack
import ws.kotonoha.model.{RepExampleStatus, WordStatus}
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.mongodb.record.{BsonListField, DelimitedStringList, PbufMessageField}
import ws.kotonoha.server.records.meta.{JodaDateField, KotonohaBsonListField, KotonohaMongoRecord, PbEnumField}
import ws.kotonoha.server.tools.JsonAstUtil


/**
 * @author eiennohito
 * @since 18.10.12 
 */
import ws.kotonoha.server.examples.api.ApiLift._
import ws.kotonoha.server.examples.ExamplesToBson._

class WordRecord private() extends MongoRecord[WordRecord] with ObjectIdPk[WordRecord] {
  def meta = WordRecord

  object writing extends DelimitedStringList(this, ",、･")
  object reading extends DelimitedStringList(this, ",、･")
  object meaning extends StringField(this, 1000) with TextAreaHtml
  object createdOn extends JodaDateField(this)
  object status extends PbEnumField(this, WordStatus, WordStatus.New)
  object tags extends BsonListField[WordRecord, String](this)

  object examples extends KotonohaBsonListField(this, ExampleRecord)
  object user extends ObjectIdRefField(this, UserRecord)
  object deleteOn extends JodaDateField(this)

  object jmdictLink extends OptionalLongField(this)
  object repExamples extends PbufMessageField[WordRecord, ExamplePack](this)
  object repExNext extends IntField(this, -1)
  object repExStatus extends PbEnumField(this, RepExampleStatus, RepExampleStatus.Fresh)

  def stripped: JValue = {
    WordRecord.trimInternal(asJValue)
  }

  override def asJValue = {
    val v = super.asJValue
    val tfed = v.transformField {
      case JField("examples", o) =>
        val res = o.transform {
          case JObject(lst) => JObject(JField("selected", JBool(true)) :: lst)
        }
        JField("examples", res)
    }
    tfed.asInstanceOf[JObject]
  }
}

object WordRecord extends WordRecord with MongoMetaRecord[WordRecord] with KotonohaMongoRecord[WordRecord] with NamedDatabase {
  def filterExamples(value: JValue) = {
    val tfed = value.transformField {
      case JField("examples", exobj) => JField("examples",
        exobj remove {
          case j: JObject => j \ "selected" match {
            case JBool(v) => !v
            case _ => true
          }
          case _ => false
        }
      )
    }.removeField {
      case JField("selected", _) => true
      case _ => false
    }
    tfed
  }

  def trimInternal(in: JValue, out: Boolean = true) = {
    val trimmed = in removeField {
      case JField("user" | "deleteOn", _) => true
      case JField("createdOn" | "status" | "_id" | "tags", _) => !out
      case _ => false
    }
    val r = JNothing ++ (if (out) trimmed else filterExamples(trimmed))
    JsonAstUtil.clean(r, saveArrays = true)
  }
}
