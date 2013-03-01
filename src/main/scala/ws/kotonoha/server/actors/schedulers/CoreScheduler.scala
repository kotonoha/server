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

package ws.kotonoha.server.actors.schedulers

import ws.kotonoha.server.actors.UserScopedActor
import org.bson.types.ObjectId
import akka.actor.{ActorLogging, Props}
import ws.kotonoha.server.records.WordCardRecord
import akka.util.Timeout
import ws.kotonoha.server.actors.learning.{WordsAndCards, LoadCards}
import akka.pattern.pipe

/**
 * @author eiennohito
 * @since 16.02.13 
 */

object State extends Enumeration {
  type State = Value
  val Initial, Normal, AfterRest, NewStarvation, ReadyStarvation, TotalStarvation = Value
}


class CoreScheduler extends UserScopedActor with ActorLogging {

  import ActorSupport._
  import concurrent.duration._

  lazy val resolver = new RepetitionStateResolver(uid)

  lazy val bad = context.actorOf(Props[BadCardScheduler], "bad")
  lazy val ready = context.actorOf(Props[ReadyCardScheduler], "ready")
  lazy val newcard = context.actorOf(Props[NewCardScheduler], "new")
  lazy val oldbal = context.actorOf(Props[OldBalancingScheduler], "oldbal")
  lazy val lowrep = context.actorOf(Props[LowRepBigIntScheduler], "lowrep")

  implicit val timeout: Timeout = 10 seconds

  val mixers = Map(
    State.Initial -> CardMixer(
      Source(ready, 1),
      Source(newcard, 1),
      Source(bad, 0.5)
    ),
    State.Normal -> CardMixer(
      Source(ready, 5),
      Source(newcard, 1),
      Source(lowrep, 0.5),
      Source(bad, 2.5)
    ),
    State.AfterRest -> CardMixer(
      Source(ready, 1),
      Source(bad, 0.5)
    ),
    State.NewStarvation -> CardMixer(
      Source(newcard, 2), //trying to get all the cards possible
      Source(ready, 1),
      Source(bad, 1),
      Source(oldbal, 0.5),
      Source(lowrep, 0.5)
    ),
    State.ReadyStarvation -> CardMixer(
      Source(newcard, 1),
      Source(ready, 2),
      Source(bad, 1),
      Source(oldbal, 1),
      Source(lowrep, 0.5)
    ),
    State.TotalStarvation -> CardMixer(
      Source(newcard, 1),
      Source(ready, 1),
      Source(bad, 1),
      Source(oldbal, 1)
    )
  )

  var current = 0

  def seqnum() = {
    val timepart = System.currentTimeMillis() / 1000
    val res = (timepart << 24) | current
    current += 1
    if (current > (1 << 24)) current = 0
    res
  }

  def receive = {
    case LoadCards(cnt) => loadCards(cnt)
  }

  def loadCards(cnt: Int) {
    val state = resolver.resolveState()
    val req = CardRequest(
      state = state,
      normalLvl = resolver.normal,
      curSession = current,
      today = resolver.today.toInt,
      limit = cnt
    )
    val mixer = mixers(state)
    log.debug("For user {} scheduler state = {}", uid, state)
    mixer.process(req).map {
      seq =>
        val outsec = seq.map {
          c => c.copy(seq = seqnum())
        }
        WordsAndCards(Nil, Nil, outsec)
    } pipeTo sender
  }
}
