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

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import com.google.inject.{Inject, Singleton}
import com.mongodb.WriteConcern
import ws.kotonoha.model.sm6.{ItemCoordinate, MatrixItem, MatrixMark}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.{OFArchiveRecord, OFElement, OFElementRecord, OFMatrixRecord}
import ws.kotonoha.server.supermemo.SuperMemo6.Crd
import ws.kotonoha.server.supermemo.SuperMemoActor.{MatrixValues, MaybeArchiveMatrix}
import ws.kotonoha.server.util.DateTimeUtils._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * @author eiennohito
  * @since 2016/08/30
  */
class SuperMemoActor @Inject() (
  uc: UserContext,
  rd: RMData
) extends Actor with ActorLogging {

  private val mat = OFMatrixRecord.forUser(uc.uid)
  private val sm = SuperMemo6.create(loadMatrix())

  def loadMatrix(): Seq[MatrixItem] = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    val els = OFElementRecord.where(_.matrix eqs mat.id.get).fetch()
    self ! MaybeArchiveMatrix(els)
    val items = els.map { r =>
      val rounded = math.round(r.ef.value).toInt
      MatrixItem(
        rounded,
        r.n.value,
        r.value.value.toFloat
      )
    }
    items
  }

  def saveItems(updates: Seq[MatrixItem]) = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    val base = OFElementRecord.where(_.matrix eqs mat.id.get)
    for (u <- updates) {
      val q = base.and(_.ef eqs u.difficulty).and(_.n eqs u.repetition).modify(_.value.setTo(u.factor))
      rd.update(q, upsert = true).onComplete {
        case Success(uwr) =>
          if (uwr.n != 1) {
            log.warning("when updating a matrix entry diff={}, n={} for user={}", u.difficulty, u.repetition, uc.uid)
          }
        case Failure(f) =>
          log.error(f, "could not update matrix item diff={}, n={} for user={}", u.difficulty, u.repetition, uc.uid)
      }(context.dispatcher)
    }
  }

  def doArchive(elements: Seq[OFElementRecord]) = {
    val items = elements map { oe =>
      OFElement(oe.n.value, oe.n.value, oe.value.value)
    }
    val ofar = OFArchiveRecord.createRecord
    ofar.elems(items.toList.sortBy(_.diff).sortBy(_.rep))
    ofar.matrix(mat.id.get)
    ofar.user(uc.uid)
    ofar.timestamp(now)
    ofar.save(WriteConcern.SAFE)
  }

  def maybeArchive(els: Seq[OFElementRecord]): Unit = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    val lastSave = OFArchiveRecord where (_.user eqs uc.uid) orderDesc (_.timestamp) select (_.timestamp) get()
    lastSave match {
      case None => doArchive(els)
      case Some(d) =>
        if (d.plusHours(20).isBeforeNow) { doArchive(els) }
    }
  }

  override def receive: Receive = {
    case m: MatrixMark =>
      val upd = sm.process(m)
      saveItems(upd.updates)
      sender() ! upd.coord
    case MaybeArchiveMatrix(els) =>
      maybeArchive(els)
    case MatrixValues =>
      sender() ! sm.snapshot()
  }
}

object SuperMemoActor {
  case class MaybeArchiveMatrix(items: Seq[OFElementRecord])
  case object MatrixValues
}

class OfMatrixSnapshot(items: Map[Crd, Double]) {
  def apply(n: Int, diff: Double): Double = {
    val crd = Crd(math.round(diff * 10).toInt, n)
    items.get(crd) match {
      case Some(v) => v
      case None => diff
    }
  }
}

@Singleton
class SuperMemoOps @Inject() (
  uc: UserContext
) {
  private val actor = uc.refFactory.actorOf(uc.props[SuperMemoActor], "supermemo")

  import akka.pattern.ask

  import scala.concurrent.duration._

  def processMark(mark: MatrixMark): Future[ItemCoordinate] = {
    implicit val timeout: Timeout = 1.second
    actor.ask(mark).mapTo[ItemCoordinate]
  }

  def values(): Future[OfMatrixSnapshot] = {
    import akka.pattern.ask
    implicit val timeout: Timeout = 1.second
    (actor ? MatrixValues).mapTo[OfMatrixSnapshot]
  }
}
