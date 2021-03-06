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

import scala.concurrent.{ExecutionContext, Future}
import org.bson.types.ObjectId
import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.scalalogging.{StrictLogging => Logging}

import scala.util.Random

/**
 * @author eiennohito
 * @since 26.02.13 
 */

trait CardSource {
  def provider: CardProvider
  def weight(req: CardRequest): Double
}

object CardSource {
  def apply(prov: CardProvider, w: Double) = new StaticCardSource(prov, w)
  def apply(prov: CardProvider, base: Double, fn: (CardRequest, Double) => Double) = new DynamicSoruce(prov, base, fn)
}

class StaticCardSource(val provider: CardProvider, w: Double) extends CardSource {
  def weight(req: CardRequest): Double = w
}

class DynamicSoruce(val provider: CardProvider, base: Double, tf: (CardRequest, Double) => Double) extends CardSource {
  def weight(req: CardRequest): Double = tf(req, base)
}

class ReqInfo(val weights: Array[Double]) {
  lazy val maxw: Double = weights.sum

  def cumulative(mask: Array[Boolean]): Array[Double] = {
    weights.zipWithIndex.map{
      case (a, i) => if (mask(i)) a else 0.0
    }.scan(0.0)(_+_)
  }
}

class CardMixer(input: List[CardSource]) extends Logging {
  import java.util.Arrays.binarySearch

  private val count = input.length
  private val rng = new Random()

  private def combine(in: Array[List[ReviewCard]], limit: Int, ri: ReqInfo): (List[Int], List[ReviewCard]) = {
    val selected = new Array[Int](count)

    val cumulative = ri.cumulative(in.map(_.nonEmpty))
    val maxvalue = cumulative.last

    def stream(trials: Int): Stream[ReviewCard] = {
      if (trials > count * 2) {
        return Stream.Empty
      }

      val v = rng.nextDouble() * maxvalue
      val crd = binarySearch(cumulative, v)
      val pos = if (crd > 0) (crd max count - 1) else -(crd + 2)
      in(pos) match {
        case x :: xs =>
          in(pos) = xs
          selected(pos) += 1
          Stream.cons(x, stream(0))
        case Nil => stream(trials + 1)
      }
    }

    try {
      val data = stream(0).distinct.take(limit).toList
      (selected.toList, data)
    } catch {
      case e: Throwable =>
        logger.error("error when selecting words", e)
        throw e
    }
  }

  def calcReqInfo(request: CardRequest): ReqInfo = {
    val weights = input.map(x => x.weight(request)).toArray
    new ReqInfo(weights)
  }

  def process(req: CardRequest)(implicit ec: ExecutionContext): Future[List[ReviewCard]] = {
    val ri = calcReqInfo(req)
    val basic = input.zip(ri.weights).map { case (src, weight) =>
      val lim = Math.ceil(req.reqLength * weight * 1.5 / ri.maxw).toInt
      src.provider.request(req.copy(reqLength = lim))
    }
    val rewrap = Future.sequence(basic)
    rewrap.map {
      lst =>
        val (cnt, data) = combine(lst.toArray, req.reqLength, ri)
        logger.debug(s"Made a selection [${req.state}] -> (${cnt.mkString(", ")})")
        cnt.zip(input).foreach {
          case (cnt, src) => src.provider.selected(cnt)
        }
        data
    }
  }
}

object CardMixer {
  def apply(srcs: CardSource*) = new CardMixer(srcs.toList)
}

case class ReviewCard(cid: ObjectId, wid: ObjectId, source: String, repId: ObjectId) {
  override def equals(obj: Any): Boolean = {
    obj match {
      case o: ReviewCard => o.cid.equals(cid) && o.repId.equals(repId)
      case _ => false
    }
  }

  override def hashCode(): Int = cid.hashCode() ^ repId.hashCode()
}

object ReviewCard {
  private val zeroId = ObjectId.createFromLegacyFormat(0, 0, 0)

  def apply(cid: ObjectId, wid: ObjectId, source: String) = new ReviewCard(cid, wid, source, zeroId)
}

case class Limits(total: Int, fresh: Int)

case class CardRequest(
  state: State.State, normalLvl: Int, curSession: Int,
  today: Int, ready: Int, border: Int, bad: Int,
  reqLength: Int, limits: Limits, base: Int, next: Seq[Int],
  ignoreWords: Seq[ObjectId] = Nil
)

case class PossibleCards(cards: List[ReviewCard])

case class CardsSelected(count: Int)

trait CardProvider {
  def request(req: CardRequest): Future[List[ReviewCard]]
  def selected(count: Int): Unit
}

object ActorSupport {

  import scala.languageFeature.implicitConversions
  import akka.pattern.ask

  implicit def actor2CardProvider(actor: => ActorRef)
                                 (implicit ec: ExecutionContext, timeout: Timeout): CardProvider = {
    new CardProvider {
      def request(req: CardRequest): Future[List[ReviewCard]] = {
        (actor ? req).mapTo[PossibleCards].map(_.cards)
      }

      def selected(count: Int) {
        actor ! CardsSelected(count)
      }
    }
  }
}
