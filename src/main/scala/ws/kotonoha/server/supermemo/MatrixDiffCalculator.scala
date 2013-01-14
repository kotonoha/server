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

package ws.kotonoha.server.supermemo

import ws.kotonoha.server.records.{MarkEventRecord, OFArchiveRecord}
import org.joda.time.DateTime
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 03.12.12 
 */

class MatrixDiffCalculator {

}

case class MatrixDiffEntry(rep: Long, ef: Double, value: Double, diff: Double, marks: List[Int])

object MatrixDiffCalculator {
  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  private case class Crd(rep: Long, diff: Double)
  def diff(left: OFArchiveRecord, right: OFArchiveRecord) = {
    val mp = left.elems.is.map {e => Crd(e.rep, e.diff) -> e.value}.toMap.withDefault(c => c.diff)
    right.elems.is.map {e => {
      val c = Crd(e.rep, e.diff)
      val old = mp(c)
      val diff = e.value - old
      e.copy(value = diff)
    }}
  }

  private def count(user: ObjectId, beg: DateTime, end: DateTime) = {
    val marks = MarkEventRecord where (_.user eqs user) and (_.datetime between(beg, end))
    var map = Map[Crd, Array[Int]]().withDefault(c => new Array[Int](5))
    marks.foreach(m => {
      val c = Crd(m.rep.is, m.diff.is)
      val o = map(c)
      o(m.mark.is.toInt - 1) += 1
      map += c -> o
    })
    map
  }

  def model(left: OFArchiveRecord, right: OFArchiveRecord) = {
    val df = diff(left, right)
    val cnt = count(left.user.is, left.timestamp.is, right.timestamp.is)
    val mp = right.elems.is.map {e => Crd(e.rep, e.diff) -> e.value}.toMap.withDefault(c => c.diff)
    df.map(e => {
      val diff = e.diff
      val rep = e.rep
      val crd = Crd(rep, diff)
      MatrixDiffEntry(rep, diff, mp(crd), e.value, cnt(crd).toList)
    })
  }
}
