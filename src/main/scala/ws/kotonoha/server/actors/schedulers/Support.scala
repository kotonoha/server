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

/**
 * @author eiennohito
 * @since 26.02.13 
 */


case class Source(function: CardRequest => Future[List[ObjectId]], weight: Double)

case class CardMixer(input: List[Source]) {

  import java.util.Arrays.binarySearch

  private val weights = input.map(_.weight).scanLeft(0.0) {
    _ + _
  } toArray

  private val maxw = weights(weights.length - 1)

  private val count = input.length

  private val rng = new Random()

  private def combine(in: Array[List[ObjectId]], limit: Int): List[ObjectId] = {
    def stream(trials: Int): Stream[ObjectId] = {
      if (trials > count * 2) {
        return Stream.Empty
      }

      val v = rng.nextDouble() * maxw
      val crd = binarySearch(weights, v)
      val pos = if (crd > 0) (crd max count - 1) else -(crd + 2)
      in(pos) match {
        case x :: xs =>
          in(pos) = xs
          Stream.cons(x, stream(0))
        case Nil => stream(trials + 1)
      }
    }

    stream(0).distinct.take(limit).toList
  }

  def process(req: CardRequest)(implicit ec: ExecutionContext): Future[List[ObjectId]] = {
    val basic = input.map(_.function(req))
    val rewrap = Future.sequence(basic)
    rewrap.map {
      lst => combine(lst.toArray, req.limit)
    }
  }
}

object CardMixer {
  def apply(srcs: Source*) = new CardMixer(srcs.toList)
}


case class CardRequest(state: State.State, normalLvl: Int, curSession: Int, today: Int, limit: Int)

case class PossibleCards(cards: List[ObjectId])

object ActorSupport {

  import scala.languageFeature.implicitConversions
  import akka.pattern.ask

  implicit def actor2RequestFunction(actor: => ActorRef)(implicit ec: ExecutionContext, timeout: Timeout):
  CardRequest => Future[List[ObjectId]] =
    (req: CardRequest) =>
      (actor ? req).mapTo[PossibleCards].map(_.cards)
}
