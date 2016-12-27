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

package ws.kotonoha.server.actors.schedulers

import akka.actor.ActorLogging
import akka.pattern.pipe
import akka.util.Timeout
import com.google.inject.Inject
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.actors.learning.WordsAndCards
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.records.misc.{CardSchedule, ScheduleRecord}

/**
 * @author eiennohito
 * @since 16.02.13 
 */

object State extends Enumeration {
  type State = Value
  val Initial, Normal, AfterRest, NewStarvation, ReadyStarvation, TotalStarvation = Value
}

object SelectMutators {
  def modifyNewSelector(part: Double, req: CardRequest): Double = {
    val count = req.next.take(3).sum + req.ready + req.today
    val lvl = req.normalLvl * 4
    val ratio = count.toDouble / lvl
    if (ratio < 1) part
    else 0
  }
}

case class LoadCardsCore(count: Int, ignoreWords: Seq[ObjectId])


class CoreScheduler @Inject() (
  uc: UserContext
) extends UserScopedActor with ActorLogging {

  import ActorSupport._
  import ws.kotonoha.server.util.DateTimeUtils._

  import concurrent.duration._

  lazy val resolver = new RepetitionStateResolver(uid)

  lazy val bad = context.actorOf(uc.props[BadCardScheduler], "bad")
  lazy val ready = context.actorOf(uc.props[ReadyCardScheduler], "ready")
  lazy val newcard = context.actorOf(uc.props[NewCardScheduler], "new")
  lazy val oldbal = context.actorOf(uc.props[OldBalancingScheduler], "oldbal")
  lazy val lowrep = context.actorOf(uc.props[LowRepBigIntScheduler], "lowrep")

  implicit val timeout: Timeout = 10 seconds

  lazy val mixers = Map(
    State.Initial -> CardMixer(
      CardSource(ready, 1),
      CardSource(newcard, 1),
      CardSource(bad, 0.5)
    ),
    State.Normal -> CardMixer(
      CardSource(ready, 5),
      CardSource(newcard, 0.3),
      CardSource(lowrep, 0.5),
      CardSource(bad, 2.5)
    ),
    State.AfterRest -> CardMixer(
      CardSource(ready, 1),
      CardSource(bad, 0.5)
    ),
    State.NewStarvation -> CardMixer(
      CardSource(newcard, 0.3),
      CardSource(ready, 1.5),
      CardSource(bad, 1),
      CardSource(oldbal, 0.5),
      CardSource(lowrep, 0.5)
    ),
    State.ReadyStarvation -> CardMixer(
      CardSource(newcard, 0.7),
      CardSource(ready, 1),
      CardSource(bad, 1),
      CardSource(oldbal, 0.2),
      CardSource(lowrep, 0.4)
    ),
    State.TotalStarvation -> CardMixer(
      CardSource(newcard, 1),
      CardSource(ready, 1),
      CardSource(bad, 1),
      CardSource(oldbal, 1)
    )
  )

  private var current = 0

  private def seqnum() = {
    current += 1
    ObjectId.get()
  }

  def dump(req: CardRequest, cards: List[ReviewCard]): Unit = {
    val date = now

    val arch = ScheduleRecord.createRecord
    arch.user(uid).date(date).cards(cards).req(req).save()

    cards.foreach { card =>
      val rec = CardSchedule.createRecord
      rec.id(card.repId).card(card.cid).source(card.source).user(uid).date(date).bundle(arch.id.get).save()
    }
  }

  def receive = {
    case LoadCardsCore(cnt, ignore) => loadCards(cnt, ignore)
    case DumpSelection(req, cards) => dump(req, cards)
  }

  def loadCards(cnt: Int, ignore: Seq[ObjectId]) {
    val state = resolver.resolveState()
    val req = CardRequest(
      state = state,
      normalLvl = resolver.normal,
      curSession = current,
      today = resolver.today.toInt,
      ready = resolver.scheduledCnt.toInt,
      border = resolver.borderline.toInt,
      bad = resolver.badCount.toInt,
      reqLength = cnt,
      limits = Limits(0, 0),
      base = resolver.lastAvg.toInt,
      next = resolver.nextTotal,
      ignoreWords = ignore
    )
    val mixer = mixers(state)
    log.debug("For user {} scheduler state = {}", uid, state)
    mixer.process(req).map {
      seq =>
        val outsec = seq.map {
          c => c.copy(repId = seqnum())
        }
        self ! DumpSelection(req, outsec)
        WordsAndCards(Nil, Nil, outsec)
    } pipeTo sender
  }
}

case class DumpSelection(req: CardRequest, cards: List[ReviewCard])
