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
import ws.kotonoha.server.util.Aggregator
import ws.kotonoha.server.records.WordCardRecord

/**
 * @author eiennohito
 * @since 27.02.13 
 */
class RepetitionStateResolver(uid: ObjectId) {

  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def pastMarks = {
    MarkEventRecord where (_.user eqs uid) and (_.datetime lt now.minusDays(1)) count()
  }

  def today = {
    MarkEventRecord where (_.user eqs uid) and (_.datetime gt now.minusDays(1)) count()
  }

  def total = pastMarks + today

  /**
   * Repetition counts for last month (except last day)
   */
  lazy val last = {
    val q = MarkEventRecord where (_.user eqs uid) and (_.datetime gt now.minusMonths(1)) and
      (_.datetime lt now.minusDays(1)) select (_.datetime)
    val end = now
    val map = q.iterate(Map[Int, Int]()) {
      case (map, Iter.Item(item)) =>
        val dist = new Duration(item, end)
        val days = dist.getStandardDays.toInt max -1
        Iter.Continue(map.updated(days, map.get(days).getOrElse(0) + 1))
      case (map, _) => Iter.Return(map)
    }

    map.toVector.sortBy(-_._1).map(_._2)
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
    val map = q.iterate(Map[Int, Int]()) {
      case (map, Iter.Item((nbef, iend))) =>
        val notBefore = nbef max iend.get
        val dur = new Duration(nowDate, notBefore)
        val days = dur.getStandardDays.toInt
        Iter.Continue(map.updated(days, map.get(days).getOrElse(0) + 1))
      case (map, _) => Iter.Return(map)
    }
    map.toList.sortBy(_._1).map(_._2)
  }

  lazy val high = {
    next.slice(1, 4).foldLeft(0) {
      _ + _
    } / 3
  }

  lazy val normal = {
    next.drop(4).foldLeft(0)(_ + _) / 6
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

  def resolveState(): State.State = {
    import State._
    val bline = normal / 10

    val noold = (newAvailable + badCount) < bline
    val nonew = newAvailable < bline
    (noold, nonew) match {
      case (true, true) => return TotalStarvation
      case (_, true) => return NewStarvation
      case (true, _) => return ReadyStarvation
      case _ => //do nothing
    }

    if (scheduledCnt > 3 * normal / 2)
      AfterRest
    else Normal
  }
}
