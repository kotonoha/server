package ws.kotonoha.server.util

import java.util.Date
import java.util.regex.Pattern

import com.mongodb.{BasicDBObjectBuilder, DBObject}
import net.liftweb.common.Full
import net.liftweb.json._
import net.liftweb.mongodb.BsonDSL
import net.liftweb.record.TypedField
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.util.matching.Regex

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
  implicit def datetime2jvalue(d: DateTime): JValue = date2jvalue(d.toDate)
  implicit def pairToOptionAssoc[A](pair: (String, A))(implicit cnv: A => JValue): JsonOptionAssoc[A] = new JsonOptionAssoc(pair)(cnv)
  implicit def jobjToOptionAssoc(obj: JObject): JsonListOptionAssoc = new JsonListOptionAssoc(obj)
  implicit def typedFieldToJobjAssoc[A](pair: (String, TypedField[A]))(implicit cnv: A => JValue): JsonListOptionAssoc = {
    pair._2.valueBox match {
      case Full(x) => new JsonListOptionAssoc(pair._1 -> cnv(x))
      case _ => new JsonListOptionAssoc(Nil)
    }
  }

  def bre(s: String) = regex2jvalue(("^" + s).r)

  class JsonOptionAssoc[A](left: (String, A))(implicit cva: A => JValue) extends JsonAssoc[A](left) {
    def ~[B](right: Option[(String, B)])(implicit cvb: B => JValue) : JObject = right match {
      case Some(r) => this ~ r
      case None => left
    }

    def ~[B](right: (String, TypedField[B]))(implicit m: Manifest[TypedField[B]]): JObject = {
      right._2.valueBox match {
        case Full(v) => new JsonAssoc(left) ~ (right._1, right._2.asJValue)
        case _ => left
      }
    }
  }

  class JsonListOptionAssoc(left: JObject) extends JsonListAssoc(left.obj) {
    def ~[A](right: Option[(String, A)])(implicit cva: A => JValue) : JObject =  right match {
      case Some(r) => this.~(r)
      case None => left
    }

    def ~[B](right: (String, TypedField[B]))(implicit cva: B => JValue) : JObject = {
      right._2.valueBox match {
        case Full(v) => new JsonListAssoc(left.obj) ~ (right._1, right._2.asJValue)
        case _ => left
      }
    }
  }

}

object KBsonDSL extends KBsonDSL
