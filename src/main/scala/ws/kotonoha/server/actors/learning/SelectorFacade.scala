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

package ws.kotonoha.server.actors.learning

import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.records.{WordRecord, WordCardRecord}
import akka.actor.{ActorLogging, Props}
import akka.actor.Status.Failure
import akka.pattern.{ask, pipe}
import scala.concurrent.duration._
import ws.kotonoha.server.actors.model.SchedulePaired
import ws.kotonoha.server.actors.schedulers.CoreScheduler

/**
 * @author eiennohito
 * @since 27.02.13
 */

class SelectorFacade extends UserScopedActor {

  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def loadReviewList(max: Int) {
    val q = WordCardRecord.enabledFor(uid) and (_.notBefore lt now.plus(1 day)) and
      (_.learning.subfield(_.repetition) eqs (1)) and
      (_.learning.subfield(_.lapse) neqs (1)) and
      (_.learning.subfield(_.intervalEnd) before (now.plus(2 days)))
    val ids = q.select(_.word).limit(max).orderDesc(_.learning.subfield(_.lapse)) fetch()
    //val wds = WordRecord.findAll("id" -> ("$in" -> ids)) map {r => r.id.is -> r} toMap
    val wds = WordRecord where (_.id in ids) fetch() map {
      r => r.id.is -> r
    } toMap
    //val s = ObjectRenderer.renderJvalue(filter)
    val ordered = ids flatMap {
      wds.get(_)
    }
    sender ! WordsAndCards(ordered, Nil)
  }

  val cards = context.actorOf(Props[CardSelectorFacade], "cards")

  def receive = {
    case LoadReviewList(max) => loadReviewList(max)
    case msg: LoadCards => cards.forward(msg)
    case msg: LoadWords => cards.forward(msg)
  }
}

class CardSelectorFacade extends UserScopedActor with ActorLogging {

  import com.foursquare.rogue.LiftRogue._

  def createResult(cards: List[WordCardRecord]): WordsAndCards = {
    val wIds = cards map (_.word.is)
    val words = WordRecord where (_.id in wIds) fetch()
    val wids = words.map(_.id.is).toSet
    val (present, absent) = cards.partition(c => wids.contains(c.word.is))
    absent.foreach(_.delete_!)
    WordsAndCards(words, present)
  }

  def processPaired(cards: List[WordCardRecord]) = {
    for (c <- cards) {
      userActor ! SchedulePaired(c.word.is, c.cardMode.is)
    }
  }

  val scheduler = context.actorOf(Props[CoreScheduler], "scheduler")

  def receive = {
    case LoadWords(max) => {
      val f = ask(self, LoadCards(max))(10 seconds).mapTo[List[WordCardRecord]]
      f.map {
        lst => createResult(lst)
      } pipeTo sender
    }
    case msg: LoadCards => {
      val f = ask(scheduler, msg)(10 seconds).mapTo[List[WordCardRecord]]
      f.map {
        lst => processPaired(lst); lst
      } pipeTo sender
    }
  }
}
