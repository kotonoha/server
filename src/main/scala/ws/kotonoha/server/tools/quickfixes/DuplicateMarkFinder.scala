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

package ws.kotonoha.server.tools.quickfixes

import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.{WordCardRecord}
import org.joda.time.DateTime
import ws.kotonoha.server.records.events.MarkEventRecord

/**
 * @author eiennohito
 * @since 08.12.12 
 */

object DuplicateMarkFinder {

  import ws.kotonoha.server.util.DateTimeUtils._
  import concurrent.duration._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def main(args: Array[String]) {
    MongoDbInit.init()

    val tendays = now minus (10 days)
    val marks = MarkEventRecord where (_.datetime gt (tendays)) fetch()

    val grps = marks.groupBy(_.datetime.is).map(x => x._1 -> x._2.toArray.sortBy(_.rep.is)).
      filter {
      x => x._2.length > 1
    }

    val toFix = grps.map(x => x._2(0)).filter(x => x.mark.is > 3.9)

    toFix.foreach(mer => {
      WordCardRecord.find(mer.card.is) foreach (c => {
        val l = c.learning.is
        l.difficulty(mer.diff.is)
        l.repetition(mer.rep.is)
        l.intervalStart(mer.datetime.is)
        val end = mer.datetime.is plus (mer.interval.is days)
        l.intervalEnd(end)
        l.intervalLength(mer.interval.is)
        c.save
      })
    })

    println(toFix.size)
  }
}
