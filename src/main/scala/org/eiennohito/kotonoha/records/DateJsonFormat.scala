package org.eiennohito.kotonoha.records

import net.liftweb.util.TimeHelpers._
import java.util.Calendar
import net.liftweb.record.field.DateTimeTypedField
import net.liftweb.record.TypedField
import net.liftweb.http.js.JE.{JsNull, Str}
import org.eiennohito.kotonoha.util.DateTimeUtils
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}
import net.liftweb.json.JsonAST.{JString, JValue}
import net.liftweb.common.{Full, Empty, Box}
import java.util


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
 * @since 07.02.12
 */

trait DateJsonFormat { this: TypedField[Calendar] with DateTimeTypedField =>

  def format(date: Calendar): String = {
    val fb = ISODateTimeFormat.basicDateTime()
    val dt = new DateTime(date.getTimeInMillis).withZone(DateTimeZone.UTC)
    fb.print(dt)
  }

  override def asJs = valueBox map { v =>
    Str(format(v))
  } openOr JsNull
  
  override def asJValue = asJString(v => format(v))

  def parse(s: String): Box[Calendar] = {
    val fb = ISODateTimeFormat.basicDateTime()
    tryo {
      val dt = fb.parseDateTime(s)
      val c = Calendar.getInstance()
      c.setTimeInMillis(dt.getMillis)
      c
    }
  }

  override def setFromJValue(jvalue: JValue): Box[MyType] = {
    jvalue match {
      case JString(s) => setBox(parse(s))
      case _ => setBox(Empty)
    }
  }

  override def setFromAny(in: Any) = {
    in match {
      case d: DateTime => setBox(Full(d.toCalendar(util.Locale.getDefault)))
      case _ => this.asInstanceOf[DateTimeTypedField].setFromAny(in)
    }
  }
}
