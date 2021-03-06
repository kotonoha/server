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

import scala.util.Random

/**
 * @author eiennohito
 * @since 08.02.13 
 */

class ReadyCardScheduler extends UserScopedActor {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  private def queryNearNow(cnt: Int, ignoredWords: Seq[ObjectId]) = {
    val q = Queries.scheduled(uid).and(_.learning.subfield(_.intervalLength) lt 7.0)
        .and(_.word nin ignoredWords)
        .orderDesc(_.learning.subfield(_.intervalEnd)).select(_.id, _.word)
    q.fetch(cnt)
  }

  private def queryNormal(cnt: Int, ignoredWords: Seq[ObjectId]) = {
    val q = Queries.scheduled(uid)
      .and(_.word nin ignoredWords)
      .orderAsc(_.learning.subfield(_.intervalEnd))
      .select(_.id, _.word)
    q.fetch(cnt)
  }

  private def interleave[T](left: List[T], right: List[T]): List[T] = {
    def rec(l: List[T], r: List[T]): List[T] = {
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
          val nearnow = queryNearNow(cnt / 2, c.ignoreWords)
          val rest = cnt - nearnow.length
          interleave(nearnow, queryNormal(rest, c.ignoreWords))
        case _ =>
          queryNormal(cnt, c.ignoreWords)
      }
      sender ! PossibleCards(data.map {
        case (cid, wid) => ReviewCard(cid, wid, "Ready")
      })
    case _: CardsSelected =>
  }
}
