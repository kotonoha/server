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

package org.eiennohito.kotonoha.learning

import org.eiennohito.kotonoha.records.WordCardRecord
import com.foursquare.rogue.Rogue
import org.joda.time.DateTime
import util.Random
import org.eiennohito.kotonoha.model.CardMode
import org.eiennohito.kotonoha.utls.DateTimeUtils
import akka.actor.{Props, Actor}
import akka.dispatch.Await
import Rogue._
import akka.pattern._
import akka.util.duration._
import DateTimeUtils._
import akka.util.Timeout

/**
 * @author eiennohito
 * @since 31.01.12
 */

class WordSelector extends Actor {
  def calculateMax(maxInt: Int, firstPerc: Double, overMax: Double) = {
    def ceil(x: Double): Int = math.round(math.ceil(x)).asInstanceOf[Int]
    val max = maxInt * (1 + overMax)
    (ceil (max * firstPerc), ceil( 2 * max * (1 - firstPerc)))
  }

  def selectCards(cardList: List[WordCardRecord], max: Int) = {
    val rnd = new Random
    val cards = new scala.collection.mutable.ArrayBuffer[WordCardRecord]()
    val col = new scala.collection.mutable.ListBuffer[WordCardRecord]
    var count = 0    
    while (count < max || cards.length == 0) {
      val idx = rnd.nextInt(cards.length)
      val item = cards.remove(idx)
      val wordId = item.word.is
      col += item
      if (item.notBefore.is.isEmpty) {
        val otherIdx = cards.zipWithIndex.filter({case (w, i) => w.word.is == wordId}).map(_._2)
        otherIdx.foreach(cards.remove(_))
        scheduler ! SchedulePaired(item.word.is, item.cardMode.is)
      }      
      count += 1
    }
    col.toList
  }

  val loaderSched = context.actorOf(Props[CardLoader], "loaderSched")
  val loaderNew = context.actorOf(Props[CardLoader], "loaderNew")
  val scheduler = context.actorOf(Props[CardScheduler], "scheduler")

  def loadNewCards(userId: Long, max: Int, now: DateTime) = {
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
    
    val list = Await.result(listF, 1.second)

    selectCards(list, max)
  }

  def forUser(userId: Long, max: Int) = {
    val now = new DateTime()
    val valid = WordCardRecord where (_.user eqs userId) and 
      (_.learning.subfield(_.intervalEnd) before now) and (_.notBefore before now) fetch(max)
    val len = valid.length
    valid ++ loadNewCards(userId, max - len, now) 
  }

  protected def receive = {
    case LoadCards(user, max) => sender ! forUser(user, max)
  }
}

case class LoadScheduled(userId: Long, maxSched: Int)
case class LoadNewCards(userId: Long, maxNew: Int)
case class LoadCards(userId: Long, max: Int)
case class SchedulePaired(wordId: Long, cardType: Int)

class CardLoader extends Actor {
  protected def receive = {
    case LoadScheduled(user, max) => {
      val scheduled = WordCardRecord where (_.user eqs user) and (_.learning exists false) and
            (_.notBefore before now) fetch (max)
      sender ! scheduled
    }
    case LoadNewCards(user, max) => {
      val newCards = WordCardRecord where (_.user eqs user) and (_.learning exists false) and
            (_.notBefore exists false) fetch (max)
      sender ! newCards
    }
  }
}

class CardScheduler extends Actor {
  protected def receive = {
    case SchedulePaired(word, cardMode) => {
      val cardType = if (cardMode == CardMode.READING) CardMode.WRITING else CardMode.READING
      val date = now plus (1.5 days)
      val q = WordCardRecord where (_.cardMode eqs cardType) and
        (_.word eqs word) findAndModify (_.notBefore setTo date)
      q.updateOne()
    }
  }
}
