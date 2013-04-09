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

/**
 *
 * This Scheduler gets bad cards - that had last mark 1, 2 or 3.
 *
 * It's not very complicated
 *
 * @author eiennohito
 * @since 08.02.13
 */
class BadCardScheduler extends UserScopedActor {

  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def loadBadCards(cnt: Int) = {
    val q = Queries.badCards(uid) select (_.id)
    q.fetch(cnt)
  }

  def receive = {
    case c: CardRequest =>
      sender ! PossibleCards(loadBadCards(c.reqLength).map {
        cid => ReviewCard(cid, "Bad")
      })
    case _: CardsSelected =>
  }
}
