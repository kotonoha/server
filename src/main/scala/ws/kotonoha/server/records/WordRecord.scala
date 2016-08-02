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

import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.mongodb.record.field._
import net.liftweb.record.field.{EnumField, DateTimeField, StringField}
import net.liftweb.json.JsonAST._
import ws.kotonoha.server.mongodb.NamedDatabase
import ws.kotonoha.server.mongodb.record.DelimitedStringList
import ws.kotonoha.server.tools.JsonAstUtil
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JBool
import net.liftweb.json.JsonAST.JObject
import ws.kotonoha.server.actors.tags.Taggable

object WordStatus extends Enumeration {
  type WordStatus = Value

  val New = Value(0)
  val Approved = Value(1)
  val ReviewWord = Value(2)
  val ReviewExamples = Value(3)
  val Deleting = Value(4)
}

/**
 * @author eiennohito
 * @since 18.10.12 
 */
class WordRecord private() extends MongoRecord[WordRecord] with ObjectIdPk[WordRecord] with Taggable {
  def meta = WordRecord

  object writing extends DelimitedStringList(this, ",、･")
  object reading extends DelimitedStringList(this, ",、･")
  object meaning extends StringField(this, 1000) with TextAreaHtml
  object createdOn extends JodaDateField(this)
  object status extends EnumField(this, WordStatus, WordStatus.New)
  object tags extends MongoListField[WordRecord, String](this)

  object examples extends BsonRecordListField(this, ExampleRecord)
  object user extends ObjectIdRefField(this, UserRecord)

  def stripped: JValue = {
    WordRecord.trimInternal(asJValue)
  }


  def curTags = tags.get

  def writeTags(tags: List[String]) {
    this.tags(tags)
  }

  override def asJValue = {
    val v = super.asJValue
    val tfed = v transform {
      case JField("examples", o) =>
        o.transform {
          case JObject(lst) => JObject(JField("selected", JBool(true)) :: lst)
        }
    }
    tfed.asInstanceOf[JObject]
  }

  object deleteOn extends JodaDateField(this)
}

object WordRecord extends WordRecord with MongoMetaRecord[WordRecord] with KotonohaMongoRecord[WordRecord] with NamedDatabase {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  def myApproved = {
    myAll and (_.status eqs WordStatus.Approved)
  }

  def myAll = {
    WordRecord where (_.user eqs UserRecord.currentId.openOrThrowException("Impossible"))
  }

  def filterExamples(value: JValue) = {
    val tfed = value transform {
      case JField("examples", exobj) => JField("examples",
        exobj remove {
          case j: JObject => j \ "selected" match {
            case JBool(v) => !v
            case _ => true
          }
          case _ => false
        }
      )
    } remove {
      case JField("selected", _) => true
      case _ => false
    }
    tfed
  }

  def trimInternal(in: JValue, out: Boolean = true) = {
    val trimmed = in remove {
      case JField("user" | "deleteOn", _) => true
      case JField("createdOn" | "status" | "_id" | "tags", _) => !out
      case _ => false
    }
    val r = JNothing ++ (if (out) trimmed else filterExamples(trimmed))
    JsonAstUtil.clean(r, saveArrays = true)
  }
}
