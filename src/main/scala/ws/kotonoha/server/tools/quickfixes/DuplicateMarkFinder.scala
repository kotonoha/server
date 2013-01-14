package ws.kotonoha.server.tools.quickfixes

import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.{WordCardRecord, MarkEventRecord}
import org.joda.time.DateTime

/**
 * @author eiennohito
 * @since 08.12.12 
 */

object DuplicateMarkFinder {
  import ws.kotonoha.server.util.DateTimeUtils._
  import concurrent.duration._
  import com.foursquare.rogue.LiftRogue._
  def main(args: Array[String]) {
    MongoDbInit.init()

    val tendays = now minus (10 days)
    val marks = MarkEventRecord where (_.datetime gt (tendays)) fetch()

    val grps = marks.groupBy(_.datetime.is).map(x => x._1 -> x._2.toArray.sortBy(_.rep.is)).
      filter{x => x._2.length > 1}

    val toFix = grps.map(x => x._2(0)).filter(x => x.mark.is > 3.9)

    toFix.foreach(mer => {
      WordCardRecord.find(mer.card.is) foreach(c => {
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
