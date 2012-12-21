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

import com.foursquare.rogue.Rogue
import org.joda.time.DateTime
import util.Random
import ws.kotonoha.server.util.DateTimeUtils
import Rogue._
import akka.pattern._
import akka.util.duration._
import akka.util.Timeout
import akka.actor.{ActorLogging, Props, Actor}
import ws.kotonoha.server.records.{WordRecord, WordCardRecord}
import ws.kotonoha.server.actors._
import model.SchedulePaired
import net.liftweb.json.JsonAST.JObject
import DateTimeUtils._
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 31.01.12
 */

class WordSelector extends Actor with ActorLogging with RootActor {
  def calculateMax(maxInt: Int, firstPerc: Double, overMax: Double) = {
    def ceil(x: Double): Int = math.round(math.ceil(x)).asInstanceOf[Int]
    val max = maxInt * (1 + overMax)
    (ceil(max * firstPerc), ceil(2 * max * (1 - firstPerc)))
  }

  def selectCards(cardList: List[WordCardRecord], max: Int) = {

    val grps = cardList.groupBy(_.word.is)
    val col = new scala.collection.mutable.ArrayBuffer[WordCardRecord]
    grps.foreach {x =>
      x._2 match {
        case v :: Nil => col += v
        case v :: vs =>  {
          col += v 
          vs.foreach {c => root ! SchedulePaired(c.word.is, c.cardMode.is)}
        }
        case _ =>
      }
    }
    val res = Random.shuffle(col).take(max)
    res.toList
  }

  val loaderSched = context.actorOf(Props[CardLoader])
  val loaderNew = context.actorOf(Props[CardLoader])

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
    
    listF map {ls => selectCards(ls, max)}
  }

  def forUser(userId: ObjectId, max: Int) = {
    val now = new DateTime()
    val valid = WordCardRecord.enabledFor(userId) and
      (_.learning.subfield(_.intervalEnd) before now) and (_.notBefore before now) orderAsc
      (_.learning.subfield(_.intervalEnd)) fetch (max)

    for (v <- valid) {
      root ! SchedulePaired(v.word.is, v.cardMode.is)
    }

    val len = valid.length

    loadNewCards(userId, max - len, now) map (valid ++ _)
  }

  def loadReviewList(user: ObjectId, max: Int) {
    val q = WordCardRecord.enabledFor(user) and (_.notBefore before now.plus(1 day)) and
      (_.learning.subfield(_.repetition) eqs (1)) and
      (_.learning.subfield(_.lapse) neqs (1)) and
      (_.learning.subfield(_.intervalEnd) before (now.plus(2 days)))
    val ids = q.select(_.word).limit(max).orderDesc(_.learning.subfield(_.lapse)) fetch()
    //val wds = WordRecord.findAll("id" -> ("$in" -> ids)) map {r => r.id.is -> r} toMap
    val wds = WordRecord where (_.id in ids) fetch() map {r => r.id.is -> r} toMap
    //val s = ObjectRenderer.renderJvalue(filter)
    val ordered = ids flatMap { wds.get(_) }
    sender ! WordsAndCards(ordered, Nil)
  }

  protected def receive = {
    case LoadReviewList(user, max) => loadReviewList(user, max)
    case LoadCards(user, max) => {
      val dest = sender
      forUser(user, max) map (dest ! _)
    }
    case LoadWords(user, max) => {
      val f = ask(self, LoadCards(user, max))(10 seconds).mapTo[List[WordCardRecord]]
      val dest = sender
      f onComplete {
        case Right(cards) =>
          dest ! createResult(cards)
        case Left(thr) =>
          log.error(thr, "Error in sending stuff")
          dest ! thr
      }
    }
  }

  def createResult(cards: List[WordCardRecord]): WordsAndCards = {
    val wIds = cards map (_.word.is)
    val words = WordRecord where (_.id in wIds) fetch()
    val wids = words.map(_.id.is).toSet
    val (present, absent) = cards.partition(c => wids.contains(c.word.is))
    absent.foreach(_.delete_!)
    WordsAndCards(words, present)
  }
}

case class LoadScheduled(userId: ObjectId, maxSched: Int)
case class LoadNewCards(userId: ObjectId, maxNew: Int)
case class LoadCards(userId: ObjectId, max: Int) extends SelectWordsMessage
case class LoadWords(userId: ObjectId, max: Int) extends SelectWordsMessage
case class WordsAndCards(words: List[WordRecord], cards: List[WordCardRecord])

case class LoadReviewList(userId: ObjectId, max: Int) extends SelectWordsMessage

class CardLoader extends Actor {
  protected def receive = {
    case LoadScheduled(user, max) => {
      val scheduled = WordCardRecord.enabledFor(user) and (_.learning exists false) and
        (_.notBefore before now) fetch (max)
      sender ! scheduled
    }
    case LoadNewCards(user, max) => {
      val newCards = WordCardRecord.enabledFor(user) and (_.learning exists false) and
        (_.notBefore exists false) orderAsc (_.createdOn) fetch (max)
      sender ! newCards
    }
  }
}
