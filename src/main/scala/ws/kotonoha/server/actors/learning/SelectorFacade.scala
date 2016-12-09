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

package ws.kotonoha.server.actors.learning

import akka.actor.{ActorLogging, Props}
import akka.pattern.{ask, pipe}
import com.google.inject.Inject
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.actors.model.SchedulePaired
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.{WordCardRecord, WordRecord}

import scala.concurrent.duration._

/**
 * @author eiennohito
 * @since 27.02.13
 */

class SelectorFacade @Inject() (
  ioc: UserContext,
  rm: RMData
) extends UserScopedActor {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def loadReviewList(max: Int) {
    val q = WordCardRecord.enabledFor(uid) and (_.notBefore lt now.plus(1 day)) and
      (_.learning.subfield(_.repetition) eqs 1) and
      (_.learning.subfield(_.lapse) neqs 1) and
      (_.learning.subfield(_.intervalEnd) before now.plus(2.days))
    val ids = q.select(_.word).limit(max).orderDesc(_.learning.subfield(_.lapse)) fetch()
    //val wds = WordRecord.findAll("id" -> ("$in" -> ids)) map {r => r.id.is -> r} toMap
    val wds = WordRecord where (_.id in ids) fetch() map {
      r => r.id.get -> r
    } toMap
    //val s = ObjectRenderer.renderJvalue(filter)
    val ordered = ids flatMap { wds.get }
    sender ! WordsAndCards(ordered, Nil, Nil)
  }

  private val cards = context.actorOf(ioc.props[CardSelectorFacade], "cards")

  def receive = {
    case LoadReviewList(max) => loadReviewList(max)
    case msg: LoadCards => cards.forward(msg)
    case msg: LoadWords => cards.forward(msg)
  }
}

class CardSelectorFacade @Inject() (
  uc: UserContext
) extends UserScopedActor with ActorLogging {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def createResult(wac: WordsAndCards): WordsAndCards = {
    val cards = wac.cards
    val wIds = cards map (_.word.get)
    val words = WordRecord where (_.id in wIds) fetch()
    val wids = words.map(_.id.get).toSet
    val (present, absent) = cards.partition(c => wids.contains(c.word.get))
    absent.foreach(_.delete_!)
    wac.copy(cards = present, words = words)
  }

  def processPaired(cards: List[WordCardRecord]) = {
    for (c <- cards) {
      userActor ! SchedulePaired(c.word.get, c.cardMode.get)
    }
  }

  private val scheduler = context.actorOf(uc.props[CardSelectorCache], "scheduler")

  def receive = {
    case LoadWords(max, skip) =>
      val f = ask(self, LoadCards(max, skip))(10 seconds).mapTo[WordsAndCards]
      f.map {
        lst => createResult(lst)
      } pipeTo sender
    case msg: LoadCards =>
      val f = ask(scheduler, msg)(10 seconds).mapTo[WordsAndCards]
      f.map {
        wac => processPaired(wac.cards); wac
      } pipeTo sender
  }
}
