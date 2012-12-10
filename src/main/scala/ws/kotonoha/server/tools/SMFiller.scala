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

package ws.kotonoha.server.tools

import ws.kotonoha.server.records.{ItemLearningDataRecord, WordCardRecord}
import ws.kotonoha.server.actors.ReleaseAkkaMain
import scala.util.Random
import org.joda.time.DateTime
import akka.actor.Props
import ws.kotonoha.server.supermemo.{ProcessMark, SM6}
import net.liftweb.common.Full
import akka.pattern.ask
import akka.util.duration._
import akka.dispatch.{Future, Await}
import akka.util.Timeout
import ws.kotonoha.server.mongodb.MongoDbInit
import org.bson.types.ObjectId
import java.util

/**
 * @author eiennohito
 * @since 21.06.12
 */

object SMFiller {
  import ws.kotonoha.server.util.DateTimeUtils._
  val userId = new ObjectId(new util.Date(30), 50, 20)

  val akka = ReleaseAkkaMain

  def createCard(d: DateTime) = {
    val rec = WordCardRecord.createRecord
    rec.user(userId).notBefore(d)
    rec.save
  }

  val rng = new Random(73L)

  def mark = {
    val i = rng.nextInt(20)
    i match {
      case v if v <= 2 => 1.0
      case v if v <= 5 => 2.0
      case v if v <= 9 => 3.0
      case v if v <= 18 => 4.0
      case _ => 5.0
    }
  }

  def select(cards: List[WordCardRecord], date: DateTime, num: Int) = {
     val rdy = cards.sortWith((c1,c2) => {
       (c1.learning.valueBox, c2.learning.valueBox) match {
         case (Full(l1), Full(l2)) => l1.intervalEnd.is.before(l2.intervalEnd.is)
         case (Full(_), _) => true
         case _ => false
       }
     }).takeWhile(_.learning.valueBox.map(_.intervalEnd.is).map(date.after(_)).openOr(false)).take(num).toSet
    (rdy.toIterator ++ cards.toIterator.withFilter(!rdy.contains(_))).take(num)
  }

  def makeMark(rec: WordCardRecord, d: DateTime): ProcessMark = {
    ProcessMark(
      rec.learning.valueBox.getOrElse(ItemLearningDataRecord.createRecord),
      mark,
      d,
      userId,
      rec.id.is
    )
  }

  def main(args: Array[String]): Unit = {
    implicit val timeout: Timeout = 5 seconds
    implicit val context = akka.system.dispatcher
    MongoDbInit.init()
    val days = 150
    val beginning = now.minusDays(days)
    val items = (1 to 5000) map {_ => createCard(beginning) } toList
    val actor = akka.system.actorOf(Props(new SM6(userId)))
    for (day <- 0 until days) {
      val today = beginning.plusDays(day)
      val futures = select(items, today, 200) map
        {i => (i, actor ? makeMark(i, today)) } map { case (i, m) =>
          m.mapTo[ItemLearningDataRecord].map { i.learning(_) } } toList ;

      Await.ready(Future.sequence(futures), 5 seconds)
    }
    akka.shutdown()
    akka.system.awaitTermination()
  }
}
