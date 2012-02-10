package org.eiennohito.kotonoha.utls

import org.joda.time.DateTime
import java.util.Calendar

import org.joda.time.{Duration => JodaDuration}
import akka.util.FiniteDuration
import net.liftweb.util.Helpers.TimeSpan
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatterBuilder, DateTimePrinter, DateTimeFormatter}

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
 * @since 01.02.12
 */

object DateTimeUtils {

  implicit def dateTime2Calendar(dt: DateTime) : Calendar = dt.toCalendar(null)
  implicit def akkaToJodaDurations(dur: FiniteDuration): JodaDuration = new JodaDuration(dur.toMillis)
  implicit def calendar2DateTime(c: Calendar) = new DateTime(c.getTimeInMillis)
  implicit def akkaDurationToLiftTimeSpan(dur: FiniteDuration) : TimeSpan = TimeSpan(dur.toMillis)

  def ts(dur: FiniteDuration) = akkaDurationToLiftTimeSpan(dur)

  def now = new DateTime()
}
