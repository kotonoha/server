/*
 * Copyright 2012-2013 eiennohito
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

import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import org.joda.time.DateTime

/**
 * @author eiennohito
 * @since 03.03.13 
 */

class DateTimeUtilsTest extends FreeSpec with ShouldMatchers {
  "datetimeutils" - {
    "snapTime" - {
      "sets time to 0500 if parameter is after that today" in {
        val dt = new DateTime(2013, 3, 3, 12, 15, 11)
        val time = DateTimeUtils.snapTime(dt)
        time should have(
          'getYear(dt.getYear()),
          'getMonthOfYear(dt.getMonthOfYear),
          'getDayOfMonth(dt.getDayOfMonth),
          'getHourOfDay(5),
          'getMinuteOfHour(0),
          'getSecondOfMinute(0)
        )
      }

      "sets time to 0500 yesterday if time is before that today" in {
        val dt = new DateTime(2013, 3, 3, 04, 15, 11)
        val time = DateTimeUtils.snapTime(dt)
        time should have(
          'getYear(dt.getYear()),
          'getMonthOfYear(dt.getMonthOfYear),
          'getDayOfMonth(dt.getDayOfMonth - 1),
          'getHourOfDay(5),
          'getMinuteOfHour(0),
          'getSecondOfMinute(0)
        )
      }
    }
  }
}
