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

package org.eiennohito.kotonoha

import org.joda.time.DateTime
import util.DateTimeUtils
import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._

/**
 * @author eiennohito
 * @since 02.03.12
 */

object MyFormat extends Formats {
  val dateFormat = new DateFormat {
    def format(d: Date) = null

    def parse(s: String) = None
  }
}

abstract class PFSerializer[A: Manifest] extends Serializer[A] {

  val Class = implicitly[Manifest[A]].erasure

  def fromJV(f: Formats): PartialFunction[JValue, A]

  def toJV(f: Formats): PartialFunction[Any, JValue]

  def deserialize(implicit format: Formats) = {
    case (TypeInfo(Class, _), json) => {
      val ser = fromJV(format)
      if (ser.isDefinedAt(json)) ser(json)
      else throw new MappingException("Can't convert " + json + " to " + Class)
    }

  }

  def serialize(implicit format: Formats) = {
    val ser = toJV(format)
    ser
  }
}

object -> {
  import JsonAST._
  def unapply(v: JValue): Option[(String, JValue)] = {
    v match {
      case obj: JObject => obj.children.head match {
        case JField(name, jv) => Some(name, jv)
        case _ => None
      }
      case _ => None
    }
  }
}


object DateTimeConverter extends PFSerializer[DateTime] {
  import JsonAST._
  def fromJV(f: Formats) = {
    case "$dt" -> JString(s) => ISODateTimeFormat.basicDateTime().parseDateTime(s)
  }

  def toJV(f: Formats) = {
    case d: DateTime => {
      import org.eiennohito.kotonoha.util.KBsonDSL._
      val s = ISODateTimeFormat.basicDateTime().print(d)
      ("$dt" -> s)
    }
  }
}

object MyEx {
  def extract() = {

  }
}

case class TestClass(d: DateTime)


class SomeTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  test("someTest") {
    val d1 = TestClass(DateTimeUtils.now)
    val o = Extraction.decompose(d1)(MyFormat + DateTimeConverter)
    val d2 = Extraction.extract[TestClass](o)(MyFormat + DateTimeConverter, Manifest.classType(classOf[TestClass]))
    val c = 1
  }

  test("wtf") {
    val str = "help me!"
    val str2 = "kill me!"

    println(str.diff(str2))
  }
}
