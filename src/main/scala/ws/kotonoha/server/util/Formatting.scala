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

package ws.kotonoha.server.util

import ws.kotonoha.server.records.UserRecord
import net.liftweb.http.RequestVar
import java.util.TimeZone
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.DateTimeFormat

/**
 * @author eiennohito
 * @since 15.03.12
 */

object Formatting {
  class MyFormatter {
    lazy val u = UserRecord.currentUser openOr UserRecord.createRecord
    lazy val locale = u.locale.isAsLocale
    lazy val tz = {
      val t = u.timezone.get
      val tz = TimeZone.getTimeZone(t)
      tz match {
        case null => DateTimeZone.forID("UTC")
        case smt => DateTimeZone.forOffsetMillis(smt.getOffset(System.currentTimeMillis()))
      }
    }

    lazy val formatter = DateTimeFormat.shortDateTime().withLocale(locale).withZone(tz)

    def formatDate(d: DateTime): String = {
      formatter.print(d)
    }
  }

  object requestFormatter extends RequestVar[MyFormatter](new MyFormatter)

  def format(in: DateTime) = requestFormatter.get.formatDate(in)

  private object dayMonF extends RequestVar[MyFormatter](new MyFormatter{
    override lazy val formatter = DateTimeFormat.forPattern("MM.dd").withLocale(locale)
  })

  def monthDay(d: DateTime) = dayMonF.get.formatDate(d)

}
