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

package ws.kotonoha.server.actors.learning

import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.learning.{CardProcessed, RegisterCardListener, UnregisterCardListener}
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.schedulers.{CoreScheduler, ReviewCard}
import akka.actor.{ActorLogging, ActorRef, Props}
import concurrent.Future
import akka.util.Timeout
import ws.kotonoha.server.records.WordCardRecord
import collection.immutable.VectorBuilder
import collection.mutable

/**
 * @author eiennohito
 * @since 01.03.13 
 */

class CardSelectorCache extends UserScopedActor with ActorLogging {

  import akka.pattern.ask
  import akka.pattern.pipe
  import concurrent.duration._

  case class ProcessAnswer(to: ActorRef, data: WordsAndCards, cnt: Int)

  implicit val timeout: Timeout = 5 seconds

  var trim: Set[ObjectId] = Set.empty

  var cache: Vector[ReviewCard] = Vector.empty

  val impl = context.actorOf(Props[CoreScheduler])

  def invalidate() = {
    cache = cache.filter(c => !trim.contains(c.cid)) //invalidate cache
    trim = Set.empty
  }

  def load(cids: Traversable[ObjectId]) = {
    import com.foursquare.rogue.LiftRogue._
    WordCardRecord where (_.id in cids) fetch()
  }

  def processLoad(cnt: Int): Unit = {
    invalidate()
    if (cache.length < cnt) {
      val s = sender
      val wnc = (impl ? LoadCards(cnt * 8 / 5)).mapTo[WordsAndCards]
      wnc.map(w => ProcessAnswer(s, w, cnt)) pipeTo self
    } else {
      answer(cnt, sender)
    }
  }


  def answer(cnt: Int, to: ActorRef) {
    val ans = cache.take(cnt)
    val cards = load(ans.map(_.cid))
    to ! WordsAndCards(Nil, cards, ans.toList)
  }

  def processReply(to: ActorRef, wnc: WordsAndCards, cnt: Int): Unit = {
    val data = wnc.sequence
    invalidate()
    val last = cache.length
    cache = (cache ++ data).distinct
    answer(cnt, to)
    log.debug("updated global card cache from {} to {} req {}", last, cache.length, cnt)
  }

  def receive = {
    case CardProcessed(cid) => trim += cid
    case LoadCards(cnt) => processLoad(cnt)
    case ProcessAnswer(to, fresh, cnt) => processReply(to, fresh, cnt)
  }

  override def preStart() {
    userActor ! RegisterCardListener(self)
    super.preStart()
  }
}
