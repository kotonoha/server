package org.eiennohito.kotonoha.supermemo

import org.eiennohito.kotonoha.records.{OFMatrixRecord, ItemLearningDataRecord}
import org.eiennohito.kotonoha.math.MathUtil
import org.joda.time.DateTime
import org.eiennohito.kotonoha.utls.DateTimeUtils
import akka.actor.{ActorRef, Actor}
import org.eiennohito.kotonoha.actors.{UpdateRecord, RegisterMongo}

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

case class ItemUpdate(data: ItemLearningDataRecord, q: Double, time: DateTime, userId: Long)

class SM6 extends Actor {

  def updateMatrix(matrix: OFMatrixRecord, item: ItemUpdate, oldEf: Double, n: Int, oldN : Int) {    
    val il = item.data.intervalLength.is
    val of = matrix.value(n, item.data.difficulty.is)
    val oldOf = matrix.value(oldN, oldEf)
    val q = item.q
    val mod5 = 1.05 max  (il + 1) / il
    val mod2 = 0.75 min  (il - 1) / il
    
    val mod = if (q > 4) {
      1 + (mod5 - 1)*(q - 4)
    } else {
      1 - (mod2 - 1) / 2 * (4 - q)
    }
    
    val oldOfval = of.value.is
    val newof = oldOf.value.is * mod
    
    val change = (q > 4 && newof > oldOfval) || (q < 4 && newof < oldOfval) 
    if (change) {
      of.value(1.2 max (oldOfval*0.9 + newof * 0.1))
      myMongo ! UpdateRecord(of)
    }
  }
  
  def updateLearningItem(item: ItemUpdate, mat: OFMatrixRecord) {
    val  q = item.q
    val data = item.data
    val raw = if (q < 3.5) {
      data.lapse(data.lapse.is + 1)
      data.repetition(1)
      mat.value(1, data.difficulty.is).value.is
    } else {
      data.repetition(data.repetition.is + 1)
      data.intervalLength.is * mat.value(data.repetition.is, data.difficulty.is).value.is
    }
    data.intervalLength(raw * MathUtil.ofrandom)
  }

  def updateDates(item: ItemUpdate) {
    import DateTimeUtils._
    val data = item.data
    val dur = MathUtil.dayToMillis (data.intervalLength.is)
    val begin = item.time
    val end = begin.plus(dur)
    
    data.intervalStart(begin)
    data.intervalEnd(end)
  }

  def update(item: ItemUpdate) : ItemLearningDataRecord = {
    val matrix = OFMatrixRecord.forUser(item.userId)
    val q = item.q
    val data = item.data
    val oldEf = item.data.difficulty.is
    val ef = 1.3 max (oldEf + 0.1 - (5 - q)*(0.08 + (5 - q)*0.02))

    if (data.repetition.is == 0) {
      data.repetition(1)
      data.lapse(1)
      data.intervalLength(4)
      //updateMatrix(matrix, item, oldEf, 1, 1)
      data.intervalLength(matrix.value(1, ef).value.is * MathUtil.ofrandom)
      updateDates(item)
      return data
    }

    data.difficulty(ef)
    val n = data.repetition.is
    updateMatrix(matrix, item, oldEf, n, if (n <= 1) 1 else n - 1)
    updateLearningItem(item, matrix)
    updateDates(item)
    data
  }

  var myMongo : ActorRef = _

  protected def receive = {
    case i: ItemUpdate => sender ! update(i)
    case RegisterMongo(mongo) => myMongo = mongo
  }
}
