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

import akka.Done
import akka.actor.{ActorLogging, ActorRef}
import akka.util.Timeout
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.actors.schedulers.{CoreScheduler, ReviewCard}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.WordCardRecord
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.records.misc.CardSchedule

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author eiennohito
 * @since 01.03.13 
 */

class CardSelectorCache @Inject() (
  uc: UserContext,
  meo: MarkEventOps,
  spo: SelectionPackOps
) extends UserScopedActor with ActorLogging {

  import akka.pattern.{ask, pipe}

  import concurrent.duration._

  case class ProcessAnswer(to: ActorRef, data: WordsAndCards, cnt: Int, skip: Int)

  implicit val timeout: Timeout = 5 seconds

  var trim: Set[ObjectId] = Set.empty

  var cache: Vector[ReviewCard] = Vector.empty

  val impl = context.actorOf(uc.props[CoreScheduler])

  def invalidate() = {
    val cnt = cache.length
    cache = cache.filter(c => !trim.contains(c.cid)) //invalidate cache
    val diff = cache.size - cnt
    if (diff != 0)
      log.debug("invalidated cache: reduced size from {} to {} by {}, trim.size {}", cnt, cache.size, diff, trim.size)
    trim = Set.empty
  }

  def load(cids: Traversable[ObjectId]) = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    WordCardRecord where (_.id in cids) fetch()
  }

  def processLoad(cnt: Int, skip: Int): Unit = {
    invalidate()
    if (cache.length < cnt) {
      val s = sender
      val wnc = (impl ? LoadCards((cnt + skip) * 6 / 5 + 5, 0)).mapTo[WordsAndCards]
      wnc.map(w => ProcessAnswer(s, w, cnt, skip)) pipeTo self
    } else {
      answer(cnt, skip, sender)
    }
  }


  def answer(cnt: Int, skip: Int, to: ActorRef) {
    val ans = cache.slice(skip, skip + cnt)
    log.debug("selected {}", ans)
    val cards = load(ans.map(_.cid))
    to ! WordsAndCards(Nil, cards, ans.toList)
  }

  def processReply(to: ActorRef, wnc: WordsAndCards, cnt: Int, skip: Int): Unit = {
    val data = wnc.sequence
    val last = cache.length
    cache = (cache ++ data).distinct
    invalidate()
    answer(cnt, skip, to)
    log.debug("updated global card cache from {} to {} req {}", last, cache.length, cnt)
  }

  def receive = {
    case CardProcessed(cid) =>
      trim += cid
      sender() ! Done
    case LoadCards(cnt, skip) => processLoad(cnt, skip)
    case ProcessAnswer(to, fresh, cnt, skip) => processReply(to, fresh, cnt, skip)
  }

  override def preStart() {
    super.preStart()
    implicit val timeout: Timeout = 1.second

    meo.addCallback { mer =>
      val f1 = self.ask(CardProcessed(mer.card.get))
      for {
        upd <- spo.updateMarkEvent(mer)
        _ <- f1
      } yield upd
    }
  }
}

class SelectionPackOps @Inject() (
  rm: RMData,
  uc: UserContext
)(implicit ec: ExecutionContext) extends StrictLogging {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.ops.OpsExtensions._

  def schedulesForCard(cid: ObjectId) = {
    val q = CardSchedule.where(_.user eqs uc.uid).and(_.card eqs cid).orderDesc(_.date)
    rm.fetch(q)
  }

  def deleteSchedules(cid: ObjectId) = {
    val q = CardSchedule.where(_.user eqs uc.uid).and(_.card eqs cid)
    rm.remove(q)
  }

  def updateMarkEvent(mr: MarkEventRecord): Future[MarkEventRecord] = {
    schedulesForCard(mr.card.get).map { scheds =>
      if (scheds.length > 1) {
        logger.warn("multiple schedules: {}", scheds)
      }

      if (scheds.nonEmpty) {
        val head = scheds.head
        mr.source(head.source.get)
        mr.seq(head.seq.get)
        mr.bundle(head.bundle.get)
        mr.scheduledOn(head.date.valueBox)

        deleteSchedules(mr.card.get).minMod(1).onFailure {
          case t => logger.warn("failed to remove schedules", t)
        }
      }

      mr
    }
  }
}
