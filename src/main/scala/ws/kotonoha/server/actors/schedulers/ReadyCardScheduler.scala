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
import util.Random

/**
 * @author eiennohito
 * @since 08.02.13 
 */

class ReadyCardScheduler extends UserScopedActor {

  import com.foursquare.rogue.LiftRogue._

  def queryNearNow(cnt: Int) = {
    val q = Queries.scheduled(uid) and (_.learning.subfield(_.intervalLength) lt 7.0) orderDesc
      (_.learning.subfield(_.intervalEnd)) select (_.id)
    q.fetch(cnt)
  }

  def queryNormal(cnt: Int) = {
    val q = Queries.scheduled(uid) orderAsc (_.learning.subfield(_.intervalEnd)) select (_.id)
    q.fetch(cnt)
  }

  def interleave(left: List[ObjectId], right: List[ObjectId]) = {
    def rec(l: List[ObjectId], r: List[ObjectId]): List[ObjectId] = {
      l match {
        case x :: xs => x :: (if (Random.nextBoolean()) rec(xs, r) else rec(r, xs))
        case Nil => r
      }
    }
    rec(left, right)
  }

  def receive = {
    case c: CardRequest =>
      val cnt = c.reqLength
      val state = c.state
      val data = state match {
        case State.AfterRest =>
          val nearnow = queryNearNow(cnt / 2)
          val rest = cnt - nearnow.length
          interleave(nearnow, queryNormal(rest))
        case _ =>
          queryNormal(cnt)
      }
      sender ! PossibleCards(data.map {
        cid => ReviewCard(cid, "Ready")
      })
    case _: CardsSelected =>
  }
}
