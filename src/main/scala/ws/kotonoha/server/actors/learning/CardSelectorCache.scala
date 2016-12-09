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

import java.util

import akka.Done
import akka.actor.{ActorLogging, ActorRef}
import akka.util.Timeout
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.bson.types.ObjectId
import org.joda.time.Duration
import reactivemongo.api.commands.WriteResult
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.actors.schedulers.{CoreScheduler, LoadCardsCore, ReviewCard}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.ops.FlashcardOps
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.records.misc.CardSchedule

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class AlreadyProcessedWordFilter {
  import ws.kotonoha.server.util.DateTimeUtils.now

  private val data = new Array[ObjectId](256)
  private val set = new mutable.HashSet[ObjectId]()
  private var idx = -1
  private var lastDate = now


  def contains(oid: ObjectId): Boolean = set.contains(oid)

  def isFresh(oid: ObjectId): Boolean = !contains(oid)


  def add(oid: ObjectId): Unit = {
    if (!contains(oid)) {
      idx = (idx + 1) & 0xff
      val prev = data(idx)
      if (prev != null) {
        set -= prev
      }

      data(idx) = oid
      set += oid
    }
  }

  def wids: Seq[ObjectId] = {
    if (new Duration(lastDate, now).getStandardHours > 1) {
      idx = -1
      set.clear()
      util.Arrays.fill(data.asInstanceOf[Array[Object]], null)
    }
    lastDate = now
    set.toSeq
  }
}


/**
 * @author eiennohito
 * @since 01.03.13 
 */
class CardSelectorCache @Inject() (
  uc: UserContext,
  meo: MarkEventOps,
  spo: SelectionPackOps,
  cops: FlashcardOps
) extends UserScopedActor with ActorLogging {

  import akka.pattern.{ask, pipe}

  import concurrent.duration._

  case class ProcessAnswer(to: ActorRef, data: WordsAndCards, cnt: Int, skip: Int)

  implicit val timeout: Timeout = 5 seconds

  private val filter = new AlreadyProcessedWordFilter


  private val processed = new mutable.HashSet[ObjectId]()
  private var cache: Vector[ReviewCard] = Vector.empty

  private val impl = context.actorOf(uc.props[CoreScheduler])


  private def cleanProcessed(): Unit = {
    cache = cache.filter(c => !processed.contains(c.cid))
    processed.clear()
  }

  def processLoad(cnt: Int, skip: Int): Unit = {
    cleanProcessed()
    if (cache.length < cnt) {
      val s = sender()
      val wnc = (impl ? LoadCardsCore((cnt + skip) * 8 / 5 + 5, filter.wids)).mapTo[WordsAndCards]
      wnc.map(w => ProcessAnswer(s, w, cnt, skip)) pipeTo self
    } else {
      answer(cnt, skip, sender())
    }
  }


  def answer(cnt: Int, skip: Int, to: ActorRef) {
    val ans = cache.slice(skip, skip + cnt)
    log.debug("cache: {} select [{}:{}] {}", cache.length, cnt, skip, ans)
    cops.forIds(ans.map(_.cid)).map { cds => WordsAndCards(Nil, cds, ans.toList) }.pipeTo(to)
  }

  def processReply(to: ActorRef, wnc: WordsAndCards, cnt: Int, skip: Int): Unit = {
    val data = wnc.sequence.filter(c => filter.isFresh(c.wid))
    data.foreach { c => filter.add(c.wid) }
    val last = cache.length
    cache = (cache ++ data).distinct
    log.debug("updated global card cache from {} to {} req {}", last, cache.length, cnt)

    answer(cnt, skip, to)
  }

  def receive = {
    case CardProcessed(cid) =>
      processed += cid
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

  def schedulesForCard(cid: ObjectId): Future[List[CardSchedule]] = {
    val q = CardSchedule.where(_.user eqs uc.uid).and(_.card eqs cid).orderDesc(_.date)
    rm.fetch(q)
  }

  def deleteSchedules(cid: ObjectId): Future[WriteResult] = {
    val q = CardSchedule.where(_.user eqs uc.uid).and(_.card eqs cid)
    rm.remove(q)
  }

  def updateMarkEvent(mr: MarkEventRecord): Future[MarkEventRecord] = {
    schedulesForCard(mr.card.get).map { scheds =>

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
