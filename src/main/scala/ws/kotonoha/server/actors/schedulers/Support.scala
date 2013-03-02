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

import concurrent.{ExecutionContext, Future}
import org.bson.types.ObjectId
import util.Random
import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author eiennohito
 * @since 26.02.13 
 */


case class Source(provider: CardProvider, weight: Double)

class CardMixer(input: List[Source]) extends Logging {

  import java.util.Arrays.binarySearch

  private val weights = input.map(_.weight).scanLeft(0.0) {
    _ + _
  } toArray

  private val maxw = weights(weights.length - 1)
  private val count = input.length
  private val rng = new Random()

  private def combine(in: Array[List[ReviewCard]], limit: Int): (List[Int], List[ReviewCard]) = {
    val selected = new Array[Int](count)

    def stream(trials: Int): Stream[ReviewCard] = {
      if (trials > count * 2) {
        return Stream.Empty
      }

      val v = rng.nextDouble() * maxw
      val crd = binarySearch(weights, v)
      val pos = if (crd > 0) (crd max count - 1) else -(crd + 2)
      in(pos) match {
        case x :: xs =>
          in(pos) = xs
          selected(pos) += 1
          Stream.cons(x, stream(0))
        case Nil => stream(trials + 1)
      }
    }

    val data = stream(0).distinct.take(limit).toList
    (selected.toList, data)
  }

  def process(req: CardRequest)(implicit ec: ExecutionContext): Future[List[ReviewCard]] = {
    val basic = input.map(src =>
      src.provider.request(req.copy(limit = req.limit * src.weight * 1.1 / maxw toInt))
    )
    val rewrap = Future.sequence(basic)
    rewrap.map {
      lst =>
        val (cnt, data) = combine(lst.toArray, req.limit)
        logger.debug(s"Made a selection [${req.state}] -> (${cnt.mkString(", ")}})")
        cnt.zip(input).foreach {
          case (cnt, src) => src.provider.selected(cnt)
        }
        data
    }
  }
}

object CardMixer {
  def apply(srcs: Source*) = new CardMixer(srcs.toList)
}

case class ReviewCard(cid: ObjectId, source: String, seq: Long) {
  override def equals(obj: Any): Boolean = {
    if (obj == null || !obj.isInstanceOf[ReviewCard]) {
      return false
    }
    val o = obj.asInstanceOf[ReviewCard]
    return cid.equals(o.cid)
  }

  override def hashCode() = cid.hashCode()
}

object ReviewCard {
  def apply(cid: ObjectId, source: String) = new ReviewCard(cid, source, 0)
}

case class CardRequest(state: State.State, normalLvl: Int, curSession: Int, today: Int, limit: Int)

case class PossibleCards(cards: List[ReviewCard])

case class CardsSelected(count: Int)

trait CardProvider {
  def request(req: CardRequest): Future[List[ReviewCard]]

  def selected(count: Int): Unit
}

object ActorSupport {

  import scala.languageFeature.implicitConversions
  import akka.pattern.ask

  implicit def actor2CardProvicer(actor: => ActorRef)
                                 (implicit ec: ExecutionContext, timeout: Timeout) = {
    new CardProvider {
      def request(req: CardRequest) = {
        (actor ? req).mapTo[PossibleCards].map(_.cards)
      }

      def selected(count: Int) {
        actor ! CardsSelected(count)
      }
    }
  }
}
