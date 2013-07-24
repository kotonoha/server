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

package ws.kotonoha.server.supermemo

import akka.actor.{ActorRef, ActorLogging, Actor}
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.server.records._
import events.MarkEventRecord
import org.joda.time.Duration
import scala.Some
import ws.kotonoha.server.math.MathUtil
import org.bson.types.ObjectId
import com.typesafe.scalalogging.slf4j.Logging
import com.mongodb.WriteConcern

/**
 * @author eiennohito
 * @since 04.12.12 
 */


case class MatrixElementUpdate(rep: Int, diff: Double, v: Double)

case class UpdateMatrix(card: ObjectId, mark: Double, interval: Double, curRep: Int, ef: Double)

class OFMatrixActor(user: ObjectId, matrix: OFMatrixHolder) extends Actor with ActorLogging {
  override def receive = {
    case MatrixElementUpdate(rep, diff, v) => matrix.update(rep, diff, v)
    case o: UpdateMatrix => updateMatrix(o)
  }

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def calcLastValues(records: List[MarkEventRecord], um: UpdateMatrix) = {
    val dr = (um.curRep - 1) max 1
    records match {
      case x :: _ => {
        var r = x.rep.valueBox.getOrElse(dr)
        var ef = x.diff.valueBox.getOrElse(um.ef)
        if (r == 0) r = dr
        if (ef == 0.0) ef = um.ef
        (r, ef)
      }
      case _ => {
        (dr, um.ef)
      }
    }
  }

  def updateMatrix(o: UpdateMatrix) {
    val n = o.curRep
    val history = MarkEventRecord where (_.card eqs o.card) orderDesc (_.datetime) skip (1) limit (n + 1) fetch()
    val (lastN, lastEf) = calcLastValues(history, o)
    val ef = o.ef
    val il = o.interval
    val of = matrix(n, ef)
    val oldOf = matrix(lastN, lastEf)
    val q = o.mark
    val mod5 = 1.05 max (il + 1) / il
    val mod2 = 0.75 min (il - 1) / il

    val mod = if (q > 4) {
      1 + (mod5 - 1) * (q - 4)
    } else {
      1 - (1 - mod2) / 2 * (4 - q)
    }

    val newof = oldOf * mod

    //log.debug("last: ({}, {})", lastN, lastEf)
    //log.debug("want to change matrix item ({}, {}) to {} from {}", ef, n, newof, of)
    val change = (q > 4 && newof > of) || (q < 4 && newof < of)
    if (change) {
      val change = 1.2 max (of * 0.9 + newof * 0.1)
      //self ! DeferredUpdate(n, ef, change)
      matrix.update(n, ef, change)
    }
  }
}

class OFMatrixHolder(user: ObjectId) extends Logging {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import DateTimeUtils._

  case class MatrixCoordinate(rep: Int, diff: Double) {
    def copyTo(in: OFElementRecord) = {
      in.ef(diff).n(rep).matrix(matrix.id.is)
    }
  }

  object Crd {
    def apply(rep: Int, diff: Double) = new MatrixCoordinate(rep, MathUtil.round(diff, 1))
  }

  private var lastSave = OFArchiveRecord where (_.user eqs user) orderDesc (_.timestamp) select (_.timestamp) get()

  private val matrix = OFMatrixRecord.forUser(user)

  private var elements: Map[MatrixCoordinate, OFElementRecord] = lookupElements(matrix)

  private def lookupElements(record: OFMatrixRecord) = {
    val elems = OFElementRecord where (_.matrix eqs matrix.id.is) fetch()
    elems.map {
      e => Crd(e.n.is, e.ef.is) -> e
    }.toMap
  }

  def apply(rep: Int, diff: Double) = synchronized {
    elements.get(Crd(rep, diff)) match {
      case Some(v) => v.value.is
      case None => diff
    }
  }

  private def archive(): Unit = {
    val items = elements map {
      case (p, v) => OFElement(p.rep, p.diff, v.value.is)
    }
    val ofar = OFArchiveRecord.createRecord
    ofar.elems(items.toList.sortBy(_.diff).sortBy(_.rep))
    ofar.matrix(matrix.id.is)
    ofar.user(user)
    ofar.timestamp(now)
    ofar.save(WriteConcern.SAFE)
    lastSave = Some(now)
  }

  private def checkArchive(): Unit = {
    lastSave match {
      case None => archive()
      case Some(t) => {
        val d = new Duration(t, now)
        if (d.getStandardDays != 0) {
          archive()
        }
      }
    }
  }

  def update(rep: Int, diff: Double, value: Double): Unit = synchronized {
    checkArchive()
    val mc = Crd(rep, diff)
    logger.debug("updating OF matrix element (%d, %f) to %f".format(mc.rep, mc.diff, value))
    elements.get(mc) match {
      case Some(el) => {
        val dbo = el.asDBObject
        val newinst = OFElementRecord.createRecord
        newinst.setFieldsFromDBObject(dbo)
        elements = elements.updated(mc, newinst)
        val q = OFElementRecord where (_.id eqs el.id.is) modify (_.value setTo (value))
        q.updateOne()
      }
      case None => {
        val el = mc.copyTo(OFElementRecord.createRecord)
        el.value(value)
        el.save
        elements += mc -> el
      }
    }
  }
}
