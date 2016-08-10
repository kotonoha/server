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

package ws.kotonoha.server.records.meta

import java.util

import com.mongodb.WriteConcern
import com.trueaccord.scalapb.GeneratedEnum
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.http.S
import net.liftweb.http.S.SFuncHolder
import net.liftweb.http.js.JE.{JsNull, Str}
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST.JValue
import net.liftweb.mongodb.MongoMeta
import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord}
import net.liftweb.record._
import net.liftweb.util.Helpers
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import reactivemongo.bson.{BSONDateTime, BSONValue}
import ws.kotonoha.server.mongodb.{ReactiveBsonMeta, ReactiveBsonSupport, ReactiveMongoMeta}
import ws.kotonoha.server.util.DateTimeUtils

import scala.xml.{NodeSeq, Null, UnprefixedAttribute}

/**
 * @author eiennohito
 * @since 14.01.13 
 */

trait JodaDateTimeTypedField[MyType <: BsonRecord[MyType]]
  extends TypedField[DateTime] with ReactiveBsonSupport {

  import net.liftweb.util.TimeHelpers._
  import ws.kotonoha.server.util.DateTimeUtils._

  def format(date: DateTime): String = {
    val fb = ISODateTimeFormat.basicDateTime()
    fb.print(date)
  }

  val formats = new DefaultFormats {
    override def dateFormatter = Helpers.internetDateFormatter
  }

  def setFromAny(in: Any): Box[DateTime] = {
    toDate(in).flatMap(d => setBox(Full(dateToCal(d)))) or genericSetFromAny(in)
  }

  override def setBox(in: Box[DateTime]): Box[DateTime] = {
    super.setBox(in.map(d => d.withZone(DateTimeUtils.UTC)))
  }

  def dateToCal(date: util.Date): DateTime = new DateTime(date, UTC)

  def setFromString(s: String): Box[DateTime] = s match {
    case null | "" if optional_? => setBox(Empty)
    case null | "" => setBox(Failure(notOptionalErrorMessage))
    case other => setBox(tryo(dateToCal(parseInternetDate(s))))
  }

  private def elem =
    S.fmapFunc(SFuncHolder(this.setFromAny(_))) {
      funcName =>
          <input type={formInputType}
                 name={funcName}
                 value={valueBox.map(s => toInternetDate(s.getTime)) openOr ""}
                 tabindex={tabIndex toString}/>
    }

  def toForm: Box[NodeSeq] =
    uniqueFieldId match {
      case Full(id) => Full(elem % new UnprefixedAttribute("id", id, Null))
      case _ => Full(elem)
    }

  def asJs = valueBox.map(v => Str(formats.dateFormat.format(v.getTime))) openOr JsNull

  def asJValue = asJString(v => format(v))

  def setFromJValue(jvalue: JValue) = setFromJString(jvalue) {
    v => tryo {
      ISODateTimeFormat.basicDateTime().parseDateTime(v).withZone(UTC)
    }
  }

  override def rbsonValue = valueBox.map(v => BSONDateTime(v.getMillis)).toStream

  override def fromRBsonValue(v: BSONValue) = v match {
    case BSONDateTime(d) => setBox(Full(new DateTime(d)))
    case _ => Failure(s"should pass DateTime instad of $v")
  }
}

class JodaDateField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
  extends Field[DateTime, OwnerType] with MandatoryTypedField[DateTime] with JodaDateTimeTypedField[OwnerType] {

  def owner = rec

  def this(rec: OwnerType, value: DateTime) = {
    this(rec)
    setBox(Full(value))
  }

  def defaultValue = DateTimeUtils.now
}

class OptionalJodaDateField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
  extends Field[DateTime, OwnerType] with OptionalTypedField[DateTime] with JodaDateTimeTypedField[OwnerType] {

  def owner = rec

  def this(rec: OwnerType, value: Box[DateTime]) = {
    this(rec)
    setBox(value)
  }
}

trait KotonohaBsonMeta[T <: BsonRecord[T]] extends BsonMetaRecord[T] with ReactiveBsonMeta[T] { self: T =>
  override def fieldDbValue(f: Field[_, T]) = {
    f.valueBox flatMap {
      case d: DateTime => Full(d)
      case v: GeneratedEnum => Full(v.value)
      case _ => super.fieldDbValue(f)
    }
  }
}

trait KotonohaMongoRecord[T <: MongoRecord[T]] extends KotonohaBsonMeta[T] with ReactiveMongoMeta[T] {
  self: T with MongoMeta[T] =>

  def bulkInsert(items: TraversableOnce[T], wc: WriteConcern = WriteConcern.ACKNOWLEDGED) = {
    val op = self.useColl(c => c.initializeUnorderedBulkOperation())
    items.foreach(i => op.insert(i.asDBObject))
    op.execute(wc)
  }

  override def createRecord: T = super.createRecord.asInstanceOf[T]
}



