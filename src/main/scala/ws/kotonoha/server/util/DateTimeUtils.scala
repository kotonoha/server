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

package ws.kotonoha.server.util

import java.util.Calendar
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import net.liftweb.util.Helpers.TimeSpan
import org.bson.types.ObjectId
import org.joda.time.{Duration => JodaDuration, _}
import ws.kotonoha.server.records.UserRecord

import scala.concurrent.duration.FiniteDuration

/**
 * @author eiennohito
 * @since 01.02.12
 */

class MultipliableDuration(val dur: JodaDuration) {
  def *(times: Int) = new JodaDuration(dur.getMillis * times)
}


object DateTimeUtils {

  import language.implicitConversions

  implicit class DateTimeComparable(val t: DateTime) extends AnyVal with Ordered[DateTime] {
    def compare(that: DateTime) = t.compareTo(that)

    def min(o: DateTime) = if (t.compareTo(o) < 0) t else o

    def max(o: DateTime) = if (t.compareTo(o) > 0) t else o
  }

  implicit def dateTime2Calendar(dt: DateTime): Calendar = dt.toCalendar(null)

  implicit def akkaToJodaDurations(dur: FiniteDuration): JodaDuration = new JodaDuration(dur.toMillis)

  implicit def calendar2DateTime(c: Calendar): DateTime = new DateTime(c.getTimeInMillis)

  implicit def akkaDurationToLiftTimeSpan(dur: FiniteDuration): TimeSpan = TimeSpan(dur.toMillis)

  implicit def liftTimeSpanToAkkaDuration(ts: TimeSpan): FiniteDuration = new FiniteDuration(ts.toMillis, TimeUnit.MILLISECONDS)

  implicit def liftTimeSpanToAkkaTimeout(ts: TimeSpan): Timeout = new Timeout(ts: FiniteDuration)

  implicit def dur2Multipliable(dur: JodaDuration): MultipliableDuration = new MultipliableDuration(dur)

  val UTC = DateTimeZone.forID("UTC")

  def ts(dur: FiniteDuration) = akkaDurationToLiftTimeSpan(dur)

  def now: DateTime = new DateTime(UTC)
  def atMillis(millis: Long): DateTime = new DateTime(millis, UTC)
  def duration(start: ReadableInstant, end: ReadableInstant) = new org.joda.time.Duration(start, end)

  def userNow(uid: Option[ObjectId]) = uid match {
    case Some(id) => {
      val dtz: DateTimeZone = usetTz(id)
      new DateTime(dtz)
    }
    case None => now
  }


  def usetTz(uid: ObjectId): DateTimeZone = {
    val u = UserRecord.find(uid)
    val tz = u map {
      _.timezone.isAsTimeZone.getOffset(System.currentTimeMillis)
    }
    val dtz = tz map {
      DateTimeZone.forOffsetMillis
    } openOr DateTimeZone.forID("UTC")
    dtz
  }

  def snapTime(uid: ObjectId): DateTime = {
    snapTime(usetTz(uid))
  }

  def snapTime(tz: DateTimeZone): DateTime = {
    val now = new DateTime(tz)
    snapTime(now)
  }

  def snapTime(now: DateTime): DateTime = {
    val time = now.withTimeAtStartOfDay().withHourOfDay(5) //05:00
    if (time.isAfter(now)) time.minusDays(1) else time
  }

  def d(date: DateTime) = date.toDate

  def tonight = {
    val dt = now
    dt.withTimeAtStartOfDay()
  }

  def last10midn = {
    intervals(tonight.minusDays(10), JodaDuration.standardDays(1), 11)
  }

  def intervals(begin: ReadableInstant, dur: JodaDuration, times: Int): List[DateTime] = {
    val beg = new DateTime(begin.getMillis)
    (0 until times).map {
      i => beg.withDurationAdded(dur, i)
    }.toList
  }
}
