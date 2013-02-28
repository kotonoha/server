package ws.kotonoha.server.actors.learning

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

import org.joda.time.DateTime
import util.Random
import ws.kotonoha.server.util.DateTimeUtils
import akka.pattern._
import concurrent.duration._
import akka.util.Timeout
import akka.actor.{ActorLogging, Props}
import ws.kotonoha.server.records.{WordRecord, WordCardRecord}
import ws.kotonoha.server.actors._
import model.SchedulePaired
import DateTimeUtils._
import org.bson.types.ObjectId
import akka.actor.Status.Failure

/**
 * @author eiennohito
 * @since 31.01.12
 */

class WordSelector extends UserScopedActor with ActorLogging {

  import com.foursquare.rogue.LiftRogue._

  def calculateMax(maxInt: Int, firstPerc: Double, overMax: Double) = {
    def ceil(x: Double): Int = math.round(math.ceil(x)).asInstanceOf[Int]
    val max = maxInt * (1 + overMax)
    (ceil(max * firstPerc), ceil(2 * max * (1 - firstPerc)))
  }

  def selectCards(cardList: List[WordCardRecord], max: Int) = {

    val grps = cardList.groupBy(_.word.is)
    val col = new scala.collection.mutable.ArrayBuffer[WordCardRecord]
    grps.foreach {
      x =>
        x._2 match {
          case v :: Nil => col += v
          case v :: vs => {
            col += v
            vs.foreach {
              c => userActor ! SchedulePaired(c.word.is, c.cardMode.is)
            }
          }
          case _ =>
        }
    }
    val res = Random.shuffle(col).take(max)
    res.toList
  }

  val loaderSched = context.actorOf(Props[CardLoader], "scheduled")
  val loaderNew = context.actorOf(Props[CardLoader], "new")

  def loadNewCards(userId: ObjectId, max: Int, now: DateTime) = {
    if (max == 0) {
      Nil
    }
    val (schedMax, newMax) = calculateMax(max, 0.7, 0.8)

    implicit val timeout: Timeout = 1.second

    val sched = loaderSched ? LoadScheduled(userId, schedMax)
    val newCards = loaderNew ? LoadNewCards(userId, newMax)

    val listF = for {
      s <- sched.mapTo[List[WordCardRecord]]
      n <- newCards.mapTo[List[WordCardRecord]]
    } yield s ++ n

    listF map {
      ls => selectCards(ls, max)
    }
  }

  def forUser(userId: ObjectId, max: Int) = {
    val valid = WordCardRecord.enabledFor(userId) and
      (_.learning.subfield(_.intervalEnd) before now) and (_.notBefore lt now) orderAsc
      (_.learning.subfield(_.intervalEnd)) fetch (max)

    val len = valid.length

    loadNewCards(userId, max - len, now) map (valid ++ _)
  }

  override def receive = {
    case LoadCards(max) => {
      forUser(uid, max) pipeTo sender
    }
  }
}

case class LoadCards(max: Int) extends SelectWordsMessage

case class LoadWords(max: Int) extends SelectWordsMessage

case class LoadReviewList(max: Int) extends SelectWordsMessage

case class WordsAndCards(words: List[WordRecord], cards: List[WordCardRecord])

case class LoadScheduled(uid: ObjectId, maxSched: Int)

case class LoadNewCards(uid: ObjectId, maxNew: Int)

class CardLoader extends UserScopedActor with ActorLogging {

  import com.foursquare.rogue.LiftRogue._

  override def receive = {
    case LoadScheduled(uid, max) => {
      val query = WordCardRecord.enabledFor(uid) and (_.learning exists false) and
        (_.notBefore lt now)
      val scheduled = query fetch (max)
      log.debug(s"Loading scheduled cards, got ${scheduled.size} of $max")
      sender ! scheduled
    }
    case LoadNewCards(uid, max) => {
      val query = WordCardRecord.enabledFor(uid) and (_.learning exists false) and
        (_.notBefore exists false) orderAsc (_.createdOn)
      val newCards = query fetch (max)
      log.debug(s"Loading new cards: got ${newCards.size} of $max")
      sender ! newCards
    }
  }
}
