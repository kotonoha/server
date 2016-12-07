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

import org.bson.types.ObjectId
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

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def loadBadCards(cnt: Int): List[(ObjectId, ObjectId)] = {
    val q = Queries.badCards(uid) select (_.id, _.word)
    q.fetch(cnt)
  }

  def receive = {
    case c: CardRequest =>
      sender ! PossibleCards(loadBadCards(c.reqLength).map {
        case (cid, wid) => ReviewCard(cid, wid, "Bad")
      })
    case _: CardsSelected =>
  }
}
