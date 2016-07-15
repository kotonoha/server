/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import net.liftweb.util.TimeHelpers._
import java.util.Calendar
import net.liftweb.record.field.DateTimeTypedField
import net.liftweb.record.TypedField
import net.liftweb.http.js.JE.{JsNull, Str}
import ws.kotonoha.server.util.DateTimeUtils
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}
import net.liftweb.json.JsonAST.{JString, JValue}
import net.liftweb.common.{Full, Empty, Box}
import java.util


/**
 * @author eiennohito
 * @since 07.02.12
 */

trait DateJsonFormat extends TypedField[util.Calendar] with DateTimeTypedField {

  def format(date: util.Calendar): String = {
    val fb = ISODateTimeFormat.basicDateTime()
    val dt = new DateTime(date.getTimeInMillis).withZone(DateTimeZone.UTC)
    fb.print(dt)
  }

  override def asJs = valueBox map { v =>
    Str(format(v))
  } openOr JsNull
  
  override def asJValue = asJString(v => format(v))

  def parse(s: String): Box[util.Calendar] = {
    val fb = ISODateTimeFormat.basicDateTime()
    tryo {
      val dt = fb.parseDateTime(s)
      val c = util.Calendar.getInstance()
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

  override def setFromAny(in: Any): Box[util.Calendar]  = {
    in match {
      case d: DateTime => setBox(Full(d.toCalendar(util.Locale.getDefault)))
      case _ => super.setFromAny(in)
    }
  }
}
