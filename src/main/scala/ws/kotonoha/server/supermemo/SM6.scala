package ws.kotonoha.server.supermemo

import ws.kotonoha.server.math.MathUtil
import ws.kotonoha.server.util.DateTimeUtils
import org.joda.time.{Duration, DateTime}
import akka.actor.{Props, ActorLogging, Actor}
import ws.kotonoha.server.records._
import akka.pattern.ask
import akka.dispatch.Await
import akka.util.Timeout

/*
 * Copyright 2012 eiennohito
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

/**
 * @author eiennohito
 * @since 30.01.12
 */

case class ItemUpdate(data: ItemLearningDataRecord, q: Double, time: DateTime, userId: Long, card: Long)

class SM6(user: Long) extends Actor with ActorLogging {
  import DateTimeUtils._

  lazy val mactor = context.actorOf(Props(new OFMatrixActor(user)))

  //F(0) = 0
  //F(0.25) = 0.5
  //F(0.5) = ~0.75
  //F(1) = 1
  //F(6.5) = 2
  //F(29) = 3
  def magicFunction(x: Double): Double = {
    math.log(1 + (math.E - 1) * math.pow(x, 0.7))
  }

  def calculateMod(time: DateTime, start: DateTime, end: DateTime) = {
    val defacto = new Duration(start, time).getMillis
    val planned = new Duration(start, end).getMillis
    val ratio = defacto.toDouble / planned.toDouble
    val f = magicFunction(ratio)
    log.debug("should have scheduled for {}, waited for {}, ratio = {}, f(ratio) = {}", planned, defacto, ratio, f)
    if (f.isInfinite || f.isNaN) {
      1.0
    } else {
      f
    }
  }

  def inertiaVal(data: ItemLearningDataRecord, q: Double) = {
    val i = data.inertia.is
    val p = q match {
      case v if v >= 3 => 0.9
      case v if v >= 2 => 0.8
      case _ => 0.6
    }
    i * p
  }

  import akka.util.duration._

  def matrix(rep: Int, ef: Double): Double = {
    implicit val timeout: Timeout = 5 seconds
    val f = (mactor ? MatrixValue(rep, ef)).mapTo[ValueResponse]
    Await.result(f, 5 seconds).v
  }

  def updateLearningItem(item: ItemUpdate) {
    val q = item.q
    val data = item.data
    var mod = calculateMod(item.time, data.intervalStart.is, data.intervalEnd.is)
    val raw = if (q < 3.5) {
      data.lapse(data.lapse.is + 1)
      data.repetition(1)
      data.inertia(inertiaVal(data, q))
      matrix(data.lapse.is, data.difficulty.is) * (0.8 max mod) * (data.inertia.is max 0.05)
    } else {
      if (data.inertia.is < 1.0) {
        data.inertia(1.0)
        data.intervalLength(matrix(data.lapse.is, data.difficulty.is))
        mod = 1.0
      }
      data.repetition(data.repetition.is + 1)
      val oldI = data.intervalLength.is
      val newI = data.intervalLength.is * matrix(data.repetition.is, data.difficulty.is)
      log.debug("updating item from {} to {} with mod {}", oldI, newI, mod)
      oldI + (newI - oldI) * mod
    }
    val interval = raw * MathUtil.ofrandom
    log.debug("Scheduling card {} in {} days, on {}", item.card, interval, item.time.plus(interval days))
    data.intervalLength(interval)
  }

  def updateDates(item: ItemUpdate) {
    val data = item.data
    val dur = MathUtil.dayToMillis (data.intervalLength.is)
    val begin = item.time
    val end = begin.plus(dur)
    
    data.intervalStart(begin)
    data.intervalEnd(end)
  }

  def update(item: ItemUpdate) : ItemLearningDataRecord = {
    val q = item.q
    val data = item.data
    val oldEf = item.data.difficulty.is
    val ef = 1.3 max (oldEf + 0.1 - (5 - q)*(0.08 + (5 - q)*0.02))

    if (data.repetition.is == 0) {
      data.repetition(1)
      data.lapse(1)
      data.intervalLength(4)
      //updateMatrix(matrix, item, oldEf, 1, 1)
      data.intervalLength(matrix(1, ef) * MathUtil.ofrandom)
      data.difficulty(ef)
      updateDates(item)
      return data
    }

    data.difficulty(ef)
    val n = data.repetition.is
    mactor ! UpdateMatrix(item.card, q, item.data.intervalLength.is, item.data.repetition.is, oldEf)
    updateLearningItem(item)
    updateDates(item)
    data
  }

  def printE[T](f : => T): T = {
    try {
      f
    } catch {
      case x: Throwable => log.error(x, "Caught an exception in sm6"); throw x
    }
  }

  protected def receive = {
    case i: ItemUpdate => sender ! printE { update(i) }
    case TerminateSM6 => context.stop(self)
  }
}
