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

import com.google.inject.Inject
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.WordCardRecord
import ws.kotonoha.server.supermemo.{OfMatrixSnapshot, SuperMemoOps}

/**
 * This one schedules cards with big intervals and low number of repetitions.
 *
 * Algorithm: we select cards that have interval as if they were marked between 2->4 and and 5->5
 * marks (intervals will be calculated with (2.1, 1) and (2.1, 2) of matrix entries).
 * Cards should be repeated at least 14 days ago.
 *
 * @author eiennohito
 * @since 27.02.13 
 */

class LowRepBigIntScheduler @Inject() (
  sm: SuperMemoOps,
  rm: RMData
) extends UserScopedActor {
  import akka.pattern.pipe
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  var of = new OfMatrixSnapshot(Map.empty)

  override def preStart(): Unit = {
    sm.values() pipeTo self
  }

  def query(cnt: Int) = {
    val lower = of(1, 2.1) * of(2, 2.1)
    val upper = of(1, 2.5) * of(2, 2.6)
    val borderline = now.minusDays(14)

    val q = WordCardRecord.enabledFor(uid) where (_.notBefore lt now) and
      (_.learning.subfield(_.intervalStart) lt borderline) and
      (_.learning.subfield(_.intervalLength) between(lower, upper)) select (_.id)
    rm.fetch(q)
  }

  def receive = {
    case s: OfMatrixSnapshot =>
      of = s
    case c: CardRequest =>
      query(c.reqLength).map{ objs =>
        PossibleCards(objs.map(cid => ReviewCard(cid, "LowRepBigInt")))
      }(context.dispatcher).pipeTo(sender())
    case _: CardsSelected =>
  }
}
