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

import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.records.WordCardRecord

/**
 * This scheduler gets cards with pretty large intervals gt 1 month that are about to
 * end in a week.
 * Maybe will extend this to a dynamic ratio in the future.
 *
 * @author eiennohito
 * @since 27.02.13 
 */
class OldBalancingScheduler extends UserScopedActor {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def query(cnt: Int) = {
    val date = now.plusDays(7)
    val q = WordCardRecord.enabledFor(uid) and (_.notBefore lt now) and
      (_.learning.subfield(_.intervalLength) gt 30.0) and
      (_.learning.subfield(_.intervalEnd) between(now, date)) select (_.id, _.word)
    q.fetch(cnt)
  }

  def receive = {
    case c: CardRequest =>
      sender ! PossibleCards(query(c.reqLength).map {
        case (cid, wid) => ReviewCard(cid, wid, "OldBalancing")
      })
    case _: CardsSelected => //
  }
}
