package ws.kotonoha.server.util

import org.bson.types.ObjectId
import java.util.regex.Pattern
import util.matching.Regex
import java.util.{Date, UUID}
import net.liftweb.json._
import ext.JodaTimeSerializers
import net.liftweb.mongodb.BsonDSL
import org.joda.time.{ReadablePartial, DateTimeZone, Chronology, DateTime}
import net.liftweb.record.TypedField
import net.liftweb.common.{Full}
import org.joda.time.format.{ISODateTimeFormat, DateTimePrinter}
import java.util
import java.io.Writer

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
 * @since 05.03.12
 */

trait KBsonDSL extends JsonDSL {
  import scala.language.implicitConversions

  val formats = DefaultFormats // ++ JodaTimeSerializers.all

  lazy val dateSaver = ISODateTimeFormat.basicDateTime()

  implicit def objectid2jvalue(oid: ObjectId): JValue = BsonDSL.objectid2jvalue(oid)
  implicit def pattern2jvalue(p: Pattern): JValue = BsonDSL.pattern2jvalue(p)
  implicit def regex2jvalue(r: Regex): JValue = BsonDSL.regex2jvalue(r)
  implicit def date2jvalue(d: Date): JValue = BsonDSL.date2jvalue(d)(formats)
  implicit def datetime2jvalue(d: DateTime) = date2jvalue(d.toDate)
  implicit def pairToOptionAssoc[A <% JValue](pair: (String, A)) = new JsonOptionAssoc(pair)
  implicit def jobjToOptionAssoc(obj: JObject) = new JsonListOptionAssoc(obj)
  implicit def typedFieldToJobjAssoc[A <% JValue](pair: (String, TypedField[A])) = pair._2.valueBox match {
    case Full(x) => new JsonListOptionAssoc(pair._1 -> x)
    case _ => new JsonListOptionAssoc(Nil)
  }

  def bre(s: String) = regex2jvalue(("^" + s).r)

  class JsonOptionAssoc[A <% JValue](left: (String, A)) extends JsonAssoc[A](left) {
    def ~[B <% JValue](right: Option[(String, B)]) : JObject = right match {
      case Some(r) => left ~ r
      case None => left
    }

    def ~[B](right: (String, TypedField[B]))(implicit m: Manifest[TypedField[B]]) : JObject = {
      right._2.valueBox match {
        case Full(v) => new JsonAssoc(left) ~ (right._1, right._2.asJValue)
        case _ => left
      }
    }
  }

  class JsonListOptionAssoc(left: JObject) extends JsonListAssoc(left.obj) {
    def ~[A <% JValue](right: Option[(String, A)]) : JObject =  right match {
      case Some(r) => new JsonListAssoc(left.obj).~(r)
      case None => left
    }

    def ~[B <% JValue](right: (String, TypedField[B])) : JObject = {
      right._2.valueBox match {
        case Full(v) => new JsonListAssoc(left.obj) ~ (right._1, right._2.asJValue)
        case _ => left
      }
    }
  }

}

object KBsonDSL extends KBsonDSL
