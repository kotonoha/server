/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

import ws.kotonoha.model.sm6.{ItemCoordinate, MatrixItem, MatrixMark, MatrixUpdate}
import ws.kotonoha.server.math.MathUtil
import ws.kotonoha.server.supermemo.SuperMemo6.Crd

import scala.collection.mutable

/**
  * @author eiennohito
  * @since 2016/08/30
  */
class SuperMemo6(items: mutable.HashMap[Crd, Double]) {
  import SuperMemo6._

  def snapshot(): OfMatrixSnapshot = new OfMatrixSnapshot(items.toMap)

  def nonFirstCoord(ef: Double, mark: MatrixMark): ItemCoordinate = {
    val q = mark.mark
    val mod = calculateMod(mark.coord.interval, mark.actualInterval)
    val crd = mark.coord
    if (q < 3.5) { //bad repetition
      val lapse = crd.lapse + 1
      val updInertia = updatedInertia(crd.inertia, q)
      ItemCoordinate(
        difficulty = ef,
        repetition = 1,
        lapse = lapse,
        interval = value(lapse, ef) * (0.8 max mod) * (updInertia max 0.05) * MathUtil.ofrandom,
        inertia = updInertia
      )
    } else {
      if (crd.inertia < 1) { //good repetition after bad
        ItemCoordinate(
          difficulty = ef,
          repetition = 1,
          inertia = 1.0,
          lapse = crd.lapse,
          interval = value(crd.lapse, ef) * MathUtil.ofrandom
        )
      } else {
        val updInerval = crd.interval * value(crd.repetition, ef)
        val intervalDiff = updInerval - crd.interval
        val newInterval = crd.interval + mod * intervalDiff
        ItemCoordinate(
          difficulty = ef,
          repetition = crd.repetition + 1,
          lapse = crd.lapse,
          interval = newInterval * MathUtil.ofrandom,
          inertia = 1.0
        )
      }
    }
  }

  def updatedCoord(mark: MatrixMark, ef: Double) = {
    if (mark.coord.repetition == 0) { //first repetition
      ItemCoordinate(
        difficulty = ef,
        repetition = 1,
        lapse = 1,
        interval = value(1, ef) * MathUtil.ofrandom,
        inertia = 1
      )
    } else {
      nonFirstCoord(ef, mark)
    }
  }

  def value(rep: Int, diff: Double): Double = {
    val dval = math.round(diff * 10.0).toInt
    val crd = Crd(dval, rep)
    items.get(crd) match {
      case Some(f) => f
      case None => diff
    }
  }

  def lastValues(mark: MatrixMark) = {
    val history = mark.history
    val computedLastN = (mark.coord.repetition - 1) max 1
    val computedLastEf = mark.coord.difficulty
    if (history.isEmpty) {
      (computedLastN, computedLastEf)
    } else {
      val i = history.head
      val actualN = i.repetition match {
        case 0 => computedLastN
        case x => x
      }
      val actualEf = i.difficulty match {
        case 0.0f => computedLastEf
        case x => x
      }
      (actualN, actualEf)
    }
  }

  def updateItem(diff: Double, rep: Int, changed: Double) = {
    val dval = math.round(diff * 10.0).toInt
    val crd = Crd(dval, rep)
    items.put(crd, changed)
    crd.item(changed)
  }

  def calculateMatrixUpdates(mark: MatrixMark, ef: Double): Seq[MatrixItem] = {
    val crd = mark.coord
    val (lastN, lastEf) = lastValues(mark)
    val interval = crd.interval
    val q = mark.mark
    val mod = if (q > 4) {
      val mod5 = 1.05 max ((interval + 1) / interval)
      1 + (mod5 - 1) * (q - 4)
    } else {
      val mod2 = 0.75 min ((interval - 1) / interval)
      1 - (1 - mod2) / 2 * (4 - q)
    }

    val oldOf = value(lastN, lastEf)
    val of = value(crd.repetition, crd.difficulty)
    val newOf = oldOf * mod
    if ((q > 4 && newOf > of) || (q < 4 && newOf < of)) {
      val changed = 1.2 max (of * 0.9 + newOf * 0.1)
      Seq(
        updateItem(crd.difficulty, crd.repetition, changed.toDouble)
      )
    } else Nil
  }

  def updateMatrix(mark: MatrixMark, ef: Double): Seq[MatrixItem] = {
    val q = mark.mark
    if (q < 4.2 && q > 3.5) return Nil //don't update 4-s

    val crd = mark.coord
    if (crd.repetition == 0) return Nil //don't update first repetitions

    if (mark.history.isEmpty) return Nil //don't update when the history is empty

    calculateMatrixUpdates(mark, ef)
  }

  def process(mark: MatrixMark): MatrixUpdate = {
    val ef = SuperMemo6.nextEf(mark)
    val newCoord = updatedCoord(mark, ef)
    val matrixUpdates = updateMatrix(mark, ef)
    MatrixUpdate(newCoord, matrixUpdates)
  }
}

object SuperMemo6 {
  def create(items: Seq[MatrixItem]) = {
    val map = new mutable.HashMap[Crd, Double]()
    items.foreach { i =>
      val diff = i.difficulty
      val crd = Crd(diff, i.repetition)
      map.put(crd, i.factor)
    }
    new SuperMemo6(map)
  }

  case class Crd(diff: Int, rep: Int) {
    def item(value: Double) = MatrixItem(diff, rep, value)
  }

  def nextEf(mark: MatrixMark): Double = {
    val q = mark.mark
    val oldEf = mark.coord.difficulty
    val ef = 1.3 max (oldEf + 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
    ef
  }

  //F(0) = 0
  //F(0.25) = 0.5
  //F(0.5) = ~0.75
  //F(1) = 1
  //F(6.5) = 2
  //F(29) = 3
  def magicFunction(x: Double): Double = {
    math.log(1 + (math.E - 1) * math.pow(x, 0.7))
  }

  def calculateMod(estimatedInterval: Double, actualInterval: Double) = {
    val ratio = actualInterval / estimatedInterval
    val f = magicFunction(ratio)
    if (java.lang.Double.isInfinite(f) || java.lang.Double.isNaN(f)) {
      1.0
    } else f
  }

  def updatedInertia(inertia: Double, q: Double): Double = {
    val p = q match {
      case v if v >= 3 => 0.9f
      case v if v >= 2 => 0.8f
      case _ => 0.6f
    }
    p * inertia
  }
}
