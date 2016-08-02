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

    val grps = marks.groupBy(_.datetime.get).map(x => x._1 -> x._2.toArray.sortBy(_.rep.get)).
      filter {
      x => x._2.length > 1
    }

    val toFix = grps.map(x => x._2(0)).filter(x => x.mark.get > 3.9)

    toFix.foreach(mer => {
      WordCardRecord.find(mer.card.get) foreach (c => {
        val l = c.learning.get
        l.difficulty(mer.diff.get)
        l.repetition(mer.rep.get)
        l.intervalStart(mer.datetime.get)
        val end = mer.datetime.get plus (mer.interval.get days)
        l.intervalEnd(end)
        l.intervalLength(mer.interval.get)
        c.save
      })
    })

    println(toFix.size)
  }
}
