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

package ws.kotonoha.server.mongodb

import java.util.Date

import com.trueaccord.scalapb.GeneratedEnum
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.record.Field
import org.bson.types.ObjectId
import org.joda.time.ReadableInstant
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson._

import scala.util.Try
import scala.util.matching.Regex

/**
  * @author eiennohito
  * @since 2016/08/10
  */
object ReactiveBson extends StrictLogging {

  def fillField(o: BsonRecord[_], f: Field[_, _], v: BSONValue): Box[_] = {
    f match {
      case support: ReactiveBsonSupport =>
        return support.fromRBsonValue(v)
      case _ =>
    }

    v match {
      case BSONObjectID(bts) => f.setFromAny(new ObjectId(bts))
      case BSONString(s) => f.setFromAny(s)
      case BSONInteger(i) => f.setFromAny(i)
      case BSONLong(i) => f.setFromAny(i)
      case BSONBoolean(i) => f.setFromAny(i)
      case BSONDouble(i) => f.setFromAny(i)
      case BSONDateTime(d) => f.setFromAny(d)
      case BSONTimestamp(l) => f.setFromAny(l)
      case BSONBinary(rb, _) => f.setFromAny(rb.readArray(rb.size))
      case BSONArray(vals) => Failure(s"setting array as field is unsupported $v")
      case d: BSONDocument => Failure(s"setting document as field is unsupported $v")
      case x: BSONRegex => Empty //ignore
      case BSONNull => Empty //ignore
      case BSONUndefined => Empty //ignore
      case _ => Failure(s"unsupported value $v")
    }
  }

  def fillObj(o: BsonRecord[_], doc: BSONDocument): Box[_] = {
    var out: Box[Failure] = Empty
    for ((name, v) <- doc.elements) {
      o.fieldByName(name) match {
        case Full(f) =>
          fillField(o, f, v) match {
            case f: Failure => out = Full(f.copy(chain = out))
            case _ =>
          }
        case _ =>
      }
    }
    if (out.isDefined) {
      out.openOrThrowException("okay")
    } else Full[BsonRecord[_]](o)
  }

  def convertObj(o: BsonRecord[_]): BSONDocument = {
    val fields = o.fields
    BSONDocument(ReactiveBson.convertFields(fields))
  }

  def convertField(fld: Field[_, _], v: Any): (String, BSONValue) = {
    (fld.name, convertValue(v))
  }

  def convertFields(fld: List[Field[_, _]]): Stream[(String, BSONValue)] = {
    def loop(rest: List[Field[_, _]]): Stream[(String, BSONValue)] = {
      rest match {
        case (x: ReactiveBsonSupport) :: xs =>
          x.rbsonValue.map(x.name -> _).append(loop(xs))
        case x :: xs =>
          val vbox = x.valueBox
          if (!x.required_? && vbox.isEmpty) {
            loop(xs)
          } else {
            vbox match {
              case Full(v) =>
                Stream.cons(convertField(x, v), loop(xs))
              case _ => loop(xs)
            }
          }
        case Nil => Stream.empty
      }
    }

    loop(fld)
  }

  def convertValue(o: Any): BSONValue = {
    o match {
      case id: ObjectId => BSONObjectID(id.toByteArray)
      case s: String => BSONString(s)
      case r: Regex => BSONRegex(r.pattern.pattern(), "") //TODO: flags
      case i: Int => BSONInteger(i)
      case l: Long => BSONLong(l)
      case f: Float => BSONDouble(f)
      case d: Double => BSONDouble(d)
      case b: Boolean => BSONBoolean(b)
      case a: Array[Byte] => BSONBinary(a, GenericBinarySubtype)
      case d: Date => BSONDateTime(d.getTime)
      case d: ReadableInstant => BSONDateTime(d.getMillis)
      case e: GeneratedEnum => BSONInteger(e.value)
      case o: BsonRecord[_] =>
        convertObj(o)
      case o: TraversableOnce[_] =>
        BSONArray(o.toStream.map(v => Try(convertValue(v))))
      case _ =>
        throw new UnsupportedOperationException(s"can't convert $o")
    }
  }
}
