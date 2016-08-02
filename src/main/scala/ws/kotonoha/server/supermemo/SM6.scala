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

package ws.kotonoha.server.supermemo

import ws.kotonoha.server.math.MathUtil
import ws.kotonoha.server.util.DateTimeUtils
import org.joda.time.{Duration, DateTime}
import akka.actor.{Props, ActorLogging}
import ws.kotonoha.server.records._
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.UserScopedActor

/**
 * @author eiennohito
 * @since 30.01.12
 */

case class ProcessMark(data: ItemLearningDataRecord, q: Double, time: DateTime, userId: ObjectId, card: ObjectId)

class SM6 extends UserScopedActor with ActorLogging {
  import DateTimeUtils._

  lazy val mactor = context.actorOf(Props(new OFMatrixActor(uid, holder)), "ofmatrix")

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
    val i = data.inertia.get
    val p = q match {
      case v if v >= 3 => 0.9
      case v if v >= 2 => 0.8
      case _ => 0.6
    }
    i * p
  }

  import concurrent.duration._

  lazy val holder = new OFMatrixHolder(uid)

  def matrix(rep: Int, ef: Double): Double = {
    holder(rep, ef)
  }

  def updateLearningItem(item: ProcessMark) {
    val q = item.q
    val data = item.data
    var mod = calculateMod(item.time, data.intervalStart.get, data.intervalEnd.get)
    val raw = if (q < 3.5) {
      //bad mark
      data.lapse(data.lapse.get + 1)
      data.repetition(1)
      data.inertia(inertiaVal(data, q))
      matrix(data.lapse.get, data.difficulty.get) * (0.8 max mod) * (data.inertia.get max 0.05)
    } else {
      if (data.inertia.get < 1.0) {
        //first good mark after bad marks
        //scheduling for simple interval (as by SM6)
        data.inertia(1.0)
        data.repetition(1)
        matrix(data.lapse.get, data.difficulty.get)
      } else {
        //sequential good mark
        data.repetition(data.repetition.get + 1)
        val oldI = data.intervalLength.get
        val newI = data.intervalLength.get * matrix(data.repetition.get, data.difficulty.get)
        log.debug("updating item from {} to {} with mod {}", oldI, newI, mod)
        oldI + (newI - oldI) * mod
      }
    }
    val interval = raw * MathUtil.ofrandom
    log.debug("Scheduling card {} in {} days, on {}", item.card, interval, item.time.plus(interval days))
    data.intervalLength(interval)
  }

  def updateDates(item: ProcessMark) {
    val data = item.data
    val dur = MathUtil.dayToMillis (data.intervalLength.get)
    val begin = item.time
    val end = begin.plus(dur)
    
    data.intervalStart(begin)
    data.intervalEnd(end)
  }

  def update(item: ProcessMark) : ItemLearningDataRecord = {
    val q = item.q
    val data = item.data
    val oldEf = item.data.difficulty.get
    val ef = 1.3 max (oldEf + 0.1 - (5 - q)*(0.08 + (5 - q)*0.02))

    if (data.repetition.get == 0) {
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
    val n = data.repetition.get
    mactor ! UpdateMatrix(item.card, q, item.data.intervalLength.get, n, oldEf)
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

  override def receive = {
    case i: ProcessMark => sender ! printE { update(i) }
  }
}
