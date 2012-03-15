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

package org.eiennohito.kotonoha.util

import org.eiennohito.kotonoha.records.UserRecord
import net.liftweb.http.RequestVar
import org.eiennohito.kotonoha.util.Fomatting.MyFormatter
import java.util.TimeZone
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

/**
 * @author eiennohito
 * @since 15.03.12
 */

object Fomatting {
  class MyFormatter {
    lazy val u = UserRecord.currentUser.openTheBox
    lazy val locale = u.locale.isAsLocale
    lazy val tz = {
      val t = u.timezone.is
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

  def format(in: DateTime) = requestFormatter.is.formatDate(in)

}
