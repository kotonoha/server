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

package ws.kotonoha.server.mongodb.record

import net.liftweb.mongodb.record.field.MongoFieldFlavor
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.common.{Failure, Empty, Box}
import net.liftweb.json.JsonAST._
import net.liftweb.record.{FieldHelpers, MandatoryTypedField, Field}
import com.mongodb.{BasicDBList, DBObject}
import net.liftweb.util.Helpers._
import net.liftweb.json.JsonParser
import net.liftweb.http.SHtml
import xml.NodeSeq
import net.liftweb.common.Full
import scala.Some
import net.liftweb.json.JsonAST.JArray
import net.liftweb.json.JsonAST.JString

/**
 * Stolen from Mongo record -- need to override one return type
 * @param rec
 * @tparam OwnerType
 * @tparam ListType
 */
private [record] class MyCoolMongoListField[OwnerType <: BsonRecord[OwnerType], ListType: Manifest](rec: OwnerType)
  extends Field[List[ListType], OwnerType]
  with MandatoryTypedField[List[ListType]]
  with MongoFieldFlavor[List[ListType]]
{
  //import Reflection._

  lazy val mf = manifest[ListType]

  override type MyType = List[ListType]

  def owner = rec

  def defaultValue = List[ListType]()

  def setFromAny(in: Any): Box[MyType] = {
    in match {
      case dbo: DBObject => setFromDBObject(dbo)
      case list@c::xs if mf.erasure.isInstance(c) => setBox(Full(list.asInstanceOf[MyType]))
      case Some(list@c::xs) if mf.erasure.isInstance(c) => setBox(Full(list.asInstanceOf[MyType]))
      case Full(list@c::xs) if mf.erasure.isInstance(c) => setBox(Full(list.asInstanceOf[MyType]))
      case s: String => setFromString(s)
      case Some(s: String) => setFromString(s)
      case Full(s: String) => setFromString(s)
      case null|None|Empty => setBox(defaultValueBox)
      case f: Failure => setBox(f)
      case o => setFromString(o.toString)
    }
  }

  def setFromJValue(jvalue: JValue) = jvalue match {
    case JNothing|JNull if optional_? => setBox(Empty)
    case JArray(arr) => setBox(Full(arr.map(_.values.asInstanceOf[ListType])))
    case other => setBox(FieldHelpers.expectedA("JArray", other))
  }

  // parse String into a JObject
  def setFromString(in: String): Box[List[ListType]] = tryo(JsonParser.parse(in)) match {
    case Full(jv: JValue) => setFromJValue(jv)
    case f: Failure => setBox(f)
    case other => setBox(Failure("Error parsing String into a JValue: "+in))
  }

  /*
   * MongoListField is built on MandatoryField, so optional_? is always false. It would be nice to use optional to differentiate
   * between a list that requires at least one item and a list that can be empty.
   */

  /** Options for select list **/
  def options: List[(ListType, String)] = Nil

  private def elem = SHtml.multiSelectObj[ListType](
    options,
    value,
    set(_)
  ) % ("tabindex" -> tabIndex.toString)

  def toForm: Box[NodeSeq] =
    if (options.length > 0)
      uniqueFieldId match {
        case Full(id) => Full(elem % ("id" -> id))
        case _ => Full(elem)
      }
    else
      Empty

  def asJValue: JValue = JArray(value.map(li => li.asInstanceOf[AnyRef] match {
    case x: String => JString(x)
    case _ => JNothing
  }))

  /*
  * Convert this field's value into a DBObject so it can be stored in Mongo.
  */
  def asDBObject: DBObject = {
    val dbl = new BasicDBList

    value.foreach {
      case f =>	f.asInstanceOf[AnyRef] match {
        case o => dbl.add(o.toString)
      }
    }
    dbl
  }

  import collection.JavaConversions._
  // set this field's value using a DBObject returned from Mongo.
  def setFromDBObject(dbo: DBObject): Box[MyType] =
    setBox(Full(dbo.asInstanceOf[BasicDBList].toList.asInstanceOf[MyType]))
}

/**
 * @author eiennohito
 * @since 18.10.12 
 */

class DelimitedStringList[Parent <: BsonRecord[Parent]] (p: Parent, delims: String = ",")
  extends MyCoolMongoListField[Parent, String] (p)
  with Field[List[String], Parent]{

  private val re = "[%s]".format(delims).r

  private val delim = {
    if (delims.length == 0)
      ","
    else
      delims.substring(0, 1)
  }

  private def parseItems(s: String): List[String] = {
    re.split(s).map { _.trim }.filter(_.length != 0).toList
  }

  def apply(s: String) = {
    setBox(Full(parseItems(s)))
    p
  }

  def apply(s : Box[String]) = {
    setBox(s map (parseItems(_)))
    p
  }

  private def setStr(in: String) = setBox(Full(parseItems(in)))

  ///use if you want to have string is, not the list one
  def stris = encode.openTheBox

  def encode = valueBox map (_.mkString(delim))

  override def setFromString(in: String) = super.setFromString(in) or setStr(in)

  override def asJValue: JValue = encode map (s => JString(s)) openOr (JNothing)

  override def setFromJValue(jvalue: JValue) = jvalue match {
    case JString(s) => setStr(s)
    case other => super.setFromJValue(other)
  }

  override def toForm = {
    encode map {
      c => SHtml.text(c, v => setFromString(v))
    }
  }
}
