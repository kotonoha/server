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

package ws.kotonoha.server.actors.schedulers

import com.foursquare.rogue.Iter
import org.bson.types.ObjectId
import ws.kotonoha.server.records.events.MarkEventRecord
import org.joda.time.Duration
import ws.kotonoha.server.util.{DateTimeUtils, Aggregator}
import ws.kotonoha.server.records.WordCardRecord

/**
 * @author eiennohito
 * @since 27.02.13 
 */
class RepetitionStateResolver(uid: ObjectId) {

  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  val snap = DateTimeUtils.snapTime(uid)

  def pastMarks = {
    MarkEventRecord where (_.user eqs uid) and (_.datetime lt snap) count()
  }

  def today = {
    MarkEventRecord where (_.user eqs uid) and (_.datetime gt snap) count()
  }

  def total = pastMarks + today

  /**
   * Repetition counts for last month (except last day)
   */
  lazy val last = {
    val end = snap
    val start = end.minusDays(30)
    val q = MarkEventRecord where (_.user eqs uid) and (_.datetime between(start, end)) select (_.datetime)
    val map = q.iterate(new Array[Int](30)) {
      case (map, Iter.Item(item)) =>
        val dist = new Duration(start, item)
        val days = dist.getStandardDays.toInt max -1
        map(days) += 1
        Iter.Continue(map)
      case (map, _) => Iter.Return(map)
    }
    map.toVector
  }

  lazy val lastStat = {
    val agg = last.foldLeft(new Aggregator()) {
      case (agg, cnt) => agg(cnt)
    }
    val mean = agg.mean
    val border = math.sqrt(agg.variance)

    val seq = last.filter {
      cnt => math.abs(cnt - mean) < border
    }
    seq.foldLeft(new Aggregator()) {
      case (x, y) => x(y)
    }
  }

  def lastAvg = lastStat.mean

  lazy val next = {
    val nowDate = now
    val date = nowDate.plusDays(10)
    val q = WordCardRecord where (_.user eqs uid) and (_.learning exists true) and
      (_.enabled eqs true) and (_.learning subfield (_.intervalEnd) lt date) select
      (_.notBefore, _.learning.subfield(_.intervalEnd))
    val init = new Array[Int](11)
    val map = q.iterateBatch(300, init) {
      case (map, Iter.Item(lst)) =>
        lst.foreach {
          case (nbef, iend) =>
            val notBefore = nbef max iend.get
            val dur = new Duration(nowDate, notBefore)
            val days = dur.getStandardDays.toInt max -1
            map(days + 1) += 1
        }
        Iter.Continue(map)
      case (map, _) => Iter.Return(map)
    }
    map.toList
  }

  lazy val high = {
    next.slice(2, 5).foldLeft(0) {
      _ + _
    } / 3
  }

  lazy val normal = {
    next.drop(5).foldLeft(0)(_ + _) / 6
  }

  def scheduledCnt = {
    Queries.scheduled(uid) count()
  }

  def newAvailable = {
    Queries.newCards(uid) count()
  }

  def badCount = {
    Queries.badCards(uid) count()
  }

  def unavailable = {
    WordCardRecord.enabledFor(uid) and (_.notBefore gt now) count()
  }

  def learnt = {
    WordCardRecord.enabledFor(uid) and (_.learning exists true) count()
  }

  def resolveState(): State.State = {
    import State._
    val bline = lastAvg / 10

    val noold = scheduledCnt < bline
    val nonew = newAvailable < bline
    (noold, nonew) match {
      case (true, true) => return TotalStarvation
      case (_, true) => return NewStarvation
      case (true, _) => return ReadyStarvation
      case _ => //do nothing
    }

    if (scheduledCnt > 3 * lastAvg / 2)
      AfterRest
    else Normal
  }
}
