package ws.kotonoha.server.records

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field._
import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord, MongoMetaRecord}
import net.liftweb.http.S
import xml.NodeSeq
import net.liftweb.common.{Full, Box, Empty}
import net.liftweb.http.S.SFuncHolder
import net.liftweb.record.field._
import net.liftweb.json.JsonAST._
import ws.kotonoha.server.util.{DateTimeUtils, ResponseUtil}
import net.liftweb.json.JsonAST.JObject
import net.liftweb.common.Full
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JBool
import org.bson.types.ObjectId
import org.joda.time.DateTime

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

/**
 * @author eiennohito
 * @since 30.01.12
 */

class ExampleRecord private() extends BsonRecord[ExampleRecord] {
  def meta = ExampleRecord

  object example extends StringField(this, 250)

  object translation extends StringField(this, 500)

  object id extends LongField(this)

}

object ExampleRecord extends ExampleRecord with BsonMetaRecord[ExampleRecord]

trait TextAreaHtml {
  self: StringTypedField =>

  import net.liftweb.util.Helpers._

  def elem(rows: Int = 5) = S.fmapFunc(SFuncHolder(this.setFromAny(_))) {
    funcName =>
      <textarea type="text" maxlength={maxLength.toString}
                name={funcName}
                rows={rows.toString}
                tabindex={tabIndex.toString}>
        {valueBox openOr ""}
      </textarea>
  }

  override def toForm: Box[NodeSeq] =
    uniqueFieldId match {
      case Full(id) => Full(elem() % ("id" -> id))
      case _ => Full(elem())
    }

}

class WordCardRecord private() extends MongoRecord[WordCardRecord] with ObjectIdPk[WordCardRecord] {
  def meta = WordCardRecord

  object cardMode extends IntField(this)

  object word extends ObjectIdRefField(this, WordRecord)

  object learning extends BsonRecordField(this, ItemLearningDataRecord, Empty) {
    override def required_? = false
  }

  object user extends ObjectIdRefField(this, UserRecord)

  object createdOn extends JodaDateField(this)

  object notBefore extends JodaDateField(this, DateTimeUtils.now)

  object enabled extends BooleanField(this, true)

  object priority extends IntField(this, 0)

  object tags extends MongoListField[WordCardRecord, String](this)

}

object WordCardRecord extends WordCardRecord with KotonohaMongoRecord[WordCardRecord] with MongoMetaRecord[WordCardRecord] with NamedDatabase {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def enabledFor(id: ObjectId) = {
    this where (_.user eqs id) and (_.enabled eqs true)
  }
}
