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

package ws.kotonoha.server.actors.learning

import akka.NotUsed
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.Full
import org.bson.types.ObjectId
import org.joda.time.DateTime
import ws.kotonoha.model.sm6.{ItemCoordinate, MatrixMark}
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.ops.FlashcardOps
import ws.kotonoha.server.records.WordCardRecord
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.supermemo.SuperMemoOps
import ws.kotonoha.server.util.DateTimeUtils._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/30
  */
@Singleton
class MarkEventOps @Inject() (
  sm: SuperMemoOps,
  fo: FlashcardOps,
  rm: RMData
)(implicit ec: ExecutionContext) extends StrictLogging {
  import MarkEventOps._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.ops.OpsExtensions._

  @volatile private[this] var callbacks: List[MarkEventHandler] = Nil

  def addCallback(meh: MarkEventHandler): Unit = callbacks = meh :: callbacks

  private def processHandlers(rec: MarkEventRecord, hndls: List[MarkEventHandler]): Future[MarkEventRecord] = {
    hndls match {
      case Nil => Future.successful(rec)
      case x :: Nil => x(rec)
      case x :: xs => x(rec).flatMap(y => processHandlers(y, xs))
    }
  }

  def historyFor(mark: MarkEventRecord) = {
    val q = MarkEventRecord.where(_.card.eqs(mark.card.get)).orderDesc(_.datetime)
    val data = rm.fetch(q)
    data
  }

  private def processImpl(mark: MarkEventRecord, card: Option[WordCardRecord], history: List[MarkEventRecord]) = {
    card match {
      case Some(c) =>
        fillMark(mark, c)

        val mobj = MatrixMark(
          coord = coord(mark),
          mark = mark.mark.value.toFloat,
          actualInterval = mark.actualInterval.value.toFloat,
          history = history.map(coord)
        )

        val f1 = sm.processMark(mobj).flatMap(crd => fo.updateLearning(c, mark.datetime.get, crd))
        val f2 = fo.schedulePaired(c.word.get, c.cardMode.get)
        val f3 = processHandlers(mark, callbacks).flatMap { mer =>
          rm.save(Seq(mer))
        }

        for {
          _ <- f1
          _ <- f2
          _ <- f3
        } yield mark
      case None =>
        throw new Exception(s"there was no card id=${mark.id.value}")
    }
  }

  def process(mark: MarkEventRecord): Future[MarkEventRecord] = {
    val cardF = fo.byId(mark.card.get)
    val historyF = historyFor(mark)

    val x = for {
      card <- cardF
      history <- historyF
      m <- processImpl(mark, card, history)
    } yield m
    x
  }

  def setReadyTime(mid: ObjectId, time: Double): Future[NotUsed] = {
    val q = MarkEventRecord.where(_.id eqs mid).modify(_.readyDur.setTo(time))
    rm.update(q).mod(1)
  }
}

object MarkEventOps {
  type MarkEventHandler = MarkEventRecord => Future[MarkEventRecord]

  val millisInDay = 1000 * 60 * 60 * 24.0

  def toDays(millis: Long): Float = (millis / millisInDay).toFloat


  def fillMark(mark: MarkEventRecord, card: WordCardRecord): MarkEventRecord = {
    val nowDate = mark.datetime.get
    mark.mode(card.cardMode.valueBox)
    card.learning.valueBox match {
      case Full(l) =>
        mark.interval(l.intervalLength.valueBox)
        mark.lapse(l.lapse.valueBox)
        mark.diff(l.difficulty.valueBox)
        mark.inertia(l.inertia.valueBox)
        mark.rep(l.repetition.valueBox)

        val ilen = duration(l.intervalStart.value, nowDate).getMillis
        mark.actualInterval(toDays(ilen))
      case _ =>
        mark.diff(2.5)
        mark.inertia(1)
    }
    mark
  }

  def coord(mark: MarkEventRecord): ItemCoordinate = {
    ItemCoordinate(
      difficulty = mark.diff.get,
      repetition = mark.rep.get,
      lapse = mark.lapse.get,
      interval = mark.interval.get,
      inertia = mark.inertia.get
    )
  }
}
