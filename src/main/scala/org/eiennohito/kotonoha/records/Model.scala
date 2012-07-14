package org.eiennohito.kotonoha.records

import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field._
import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord, MongoMetaRecord}
import net.liftweb.http.S
import xml.NodeSeq
import net.liftweb.common.{Full, Box, Empty}
import net.liftweb.http.S.SFuncHolder
import net.liftweb.record.field._

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
                tabindex={tabIndex.toString}>{valueBox openOr ""}</textarea>
  }

  override def toForm: Box[NodeSeq] =
    uniqueFieldId match {
      case Full(id) => Full(elem() % ("id" -> id))
      case _ => Full(elem())
    }

}

object WordStatus extends Enumeration {
  type WordStatus = Value

  val New = Value(0)
  val Approved = Value(1)
  val ReviewWord = Value(2)
  val ReviewExamples = Value(3)
  val Deleting = Value(4)
}

class WordRecord private() extends MongoRecord[WordRecord] with LongPk[WordRecord] with SequencedLongId[WordRecord] {
  def meta = WordRecord

  object writing extends StringField(this, 100)
  object reading extends StringField(this, 150)
  object meaning extends StringField(this, 1000) with TextAreaHtml
  object createdOn extends DateTimeField(this) with DateJsonFormat
  object status extends EnumField(this, WordStatus, WordStatus.New)
  object tags extends MongoListField[WordRecord, String](this)

  object examples extends BsonRecordListField(this, ExampleRecord)
  object user extends LongRefField(this, UserRecord)

  object deleteOn extends DateTimeField(this) with DateJsonFormat
}

object WordRecord extends WordRecord with MongoMetaRecord[WordRecord] with NamedDatabase {
  import com.foursquare.rogue.Rogue._
  def myApproved = {
    myAll and (_.status eqs WordStatus.Approved)
  }

  def myAll = {
    WordRecord where (_.user eqs UserRecord.currentId.openTheBox)
  }
}

class WordCardRecord private() extends MongoRecord[WordCardRecord] with LongPk[WordCardRecord] {
  def meta = WordCardRecord

  object cardMode extends IntField(this)
  object word extends LongRefField(this, WordRecord)
  object learning extends BsonRecordField(this, ItemLearningDataRecord, Empty) {
    override def required_? = false
  }
  object user extends LongRefField(this, UserRecord)
  object createdOn extends DateTimeField(this) with DateJsonFormat
  object notBefore extends OptionalDateTimeField(this, Empty) with DateJsonFormat
  object enabled extends BooleanField(this, true)
}

object WordCardRecord extends WordCardRecord with MongoMetaRecord[WordCardRecord] with NamedDatabase {
  import com.foursquare.rogue.Rogue._

  def enabledFor(id: Long) = {
    this where (_.user eqs id) and (_.enabled eqs true)
  }
}
