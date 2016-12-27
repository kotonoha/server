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
import ws.kotonoha.examples.api.ExampleSentence
import ws.kotonoha.model.sm6.{ItemCoordinate, MatrixMark}
import ws.kotonoha.server.actors.learning.RepeatBackend.RepExReport
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.ops.{FlashcardOps, WordOps}
import ws.kotonoha.server.records.{ExampleRecord, WordCardRecord}
import ws.kotonoha.server.records.events.{ExampleStatusReport, MarkEventRecord}
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
  wo: WordOps,
  uc: UserContext,
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

  private def processImpl(mark: MarkEventRecord, card: Option[WordCardRecord], history: List[MarkEventRecord], ex: Option[ExampleSentence]) = {
    card match {
      case Some(c) =>
        fillMark(mark, c)

        val mobj = MatrixMark(
          coord = coord(mark),
          mark = mark.mark.value.toFloat,
          actualInterval = mark.actualInterval.value.toFloat,
          history = history.map(coord)
        )

        val f4 = ex match {
          case None => Future.successful(NotUsed)
          case Some(e) =>
            mark.wordExId(e.id.toByteArray)
            wo.markUsedExample(card.get.word.get, mark.wordExIdx.get)
        }

        val f1 = sm.processMark(mobj).flatMap(crd => fo.updateLearning(c, mark.datetime.get, crd))
        val f2 = fo.schedulePaired(c.word.get, c.cardMode.get)
        val f3 = processHandlers(mark, callbacks).flatMap { mer =>
          rm.save(Seq(mer))
        }


        for {
          _ <- f1
          _ <- f2
          _ <- f3
          _ <- f4
        } yield mark
      case None =>
        throw new Exception(s"there was no card id=${mark.id.value}")
    }
  }

  def process(mark: MarkEventRecord): Future[MarkEventRecord] = {
    val cardF = fo.byId(mark.card.get)
    val historyF = historyFor(mark)
    val repExF = repSentence(mark, cardF)
    val x = for {
      card <- cardF
      history <- historyF
      ex <- repExF
      m <- processImpl(mark, card, history, ex)
    } yield m
    x
  }

  private def repSentence(mark: MarkEventRecord, cardF: Future[Option[WordCardRecord]]) = {
    if (mark.wordExIdx.get == -1) Future.successful(None)
    else cardF.flatMap {
      case None => Future.successful(None)
      case Some(s) => wo.byId(s.word.get).map(_.flatMap(w => w.repExamples.valueBox match {
        case Full(re) =>
          val idx = mark.wordExIdx.get
          if (idx >= 0 && idx < re.sentences.length) {
            Some(re.sentences(idx))
          } else None
        case _ => None
      }))
    }
  }

  def setReadyTime(mid: ObjectId, time: Double): Future[NotUsed] = {
    val q = MarkEventRecord.where(_.id eqs mid).modify(_.readyDur.setTo(time))
    rm.update(q).mod(1)
  }

  def reportRepSentence(data: RepExReport): Future[NotUsed] = {
    val searchq = ExampleStatusReport.where(_.id eqs data.repId)
    rm.count(searchq).flatMap {
      case 1 =>
        val updq = searchq.modify(_.timestamp.setTo(now)).modify(_.status.setTo(data.status))
        rm.update(updq).mod(1)
      case 0 =>
        val word = wo.byId(data.wordId)
        word.flatMap {
          case Some(w) =>
            val rec = ExampleStatusReport.createRecord
            rec.id(data.repId)
            rec.timestamp(now)
            rec.user(uc.uid)
            rec.wordId(w.id.get)
            val exs = w.repExamples.get
            rec.example(exs.sentences(data.exId))
            rec.status(data.status)
            rm.updateOne(rec).map(_ => NotUsed)
          case _ => Future.successful(NotUsed)
        }
    }

  }
}

object MarkEventOps {
  type MarkEventHandler = MarkEventRecord => Future[MarkEventRecord]

  val millisInDay: Double = 1000 * 60 * 60 * 24.0

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
