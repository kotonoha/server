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

package ws.kotonoha.server.ops

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Source}
import akka.stream.{OverflowStrategy, SourceShape}
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.bson.types.ObjectId
import ws.kotonoha.akane.dic.jmdict.{JMDictUtil, JmdictEntry, JmdictTag}
import ws.kotonoha.akane.utils.timers.{Millis, Millitimer}
import ws.kotonoha.dict.jmdict.LuceneJmdict
import ws.kotonoha.examples.api.{ExamplePack, ExamplePackRequest, ExampleQuery, ExampleTag}
import ws.kotonoha.model.RepExampleStatus
import ws.kotonoha.server.actors.examples.ExampleAssignmentStatus
import ws.kotonoha.server.actors.schedulers.SchedulingOps
import ws.kotonoha.server.ioc.UserContextService
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.util.stream.SummaryStage

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * @author eiennohito
  * @since 2016/08/23
  */
class WordExampleOps @Inject() (
  ec: ExampleCacher,
  jmd: LuceneJmdict,
  wjo: WordJmdictOps,
  rd: RMData,
  ucs: UserContextService
)(implicit ex: ExecutionContextExecutor) extends StrictLogging {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  /**
    * Logic of this graph:
    * concatenate:
    *   1) words in future 200 repetitions
    *   2) all other words
    *
    * First block of words is created by getting a list of cards from scheduler.
    * Cards then are serve as source of individual queries to the db.
    *
    * Second block of words is a simple large query to the db.
    */
  def wordsForAssign(uid: ObjectId): Source[WordRecord, NotUsed] = {
    val uc = ucs.of(uid)
    val sch = uc.inst[SchedulingOps]

    import GraphDSL.Implicits._

    val grph = GraphDSL.create() { implicit b =>
      val s1 = b.add(sch.cardsRepeatedInFuture(200).flatMapConcat { wc =>
        val q = WordRecord.where(_.user eqs uid).and(_.id eqs wc.word.get).and(_.repExStatus.neqs(RepExampleStatus.Present))
        rd.stream(q).map { w => logger.trace("q={} id={} {}", q, w.id.get, w.writing.stris); w}
      }.buffer(5, OverflowStrategy.backpressure))
      val bc = b.add(Broadcast[WordRecord](2, eagerCancel = false))
      val conc = b.add(Concat[WordRecord](2))
      val summary = b.add(new SummaryStage[ObjectId])
      s1 ~> bc.in
      bc.map(_.id.get) ~> summary

      val p2 = b.add {
        Flow[Seq[ObjectId]].flatMapConcat { ids =>
          logger.trace("ignoring {} words", ids.size)
          val q = WordRecord.where(_.user eqs uid).and(_.repExStatus.neqs(RepExampleStatus.Present)).and(_.id nin ids)
          rd.stream(q)
        }.map(x => { logger.trace("rest: id={} {}", x.id, x.writing.stris); x })
      }

      summary ~> p2
      bc ~> conc
      p2 ~> conc

      SourceShape(conc.out)
    }

    Source.fromGraph(grph)
  }

  def assignWordsById(wid: ObjectId): Future[ExampleAssignmentStatus] = {
    rd.byId[WordRecord](wid).flatMap {
      case None => Future.successful(ExampleAssignmentStatus(ObjectId.createFromLegacyFormat(0, 0, 0), wid, -1, Millis(0L)))
      case Some(s) => findAndAssign(s)
    }
  }

  def findAndAssign(w: WordRecord): Future[ExampleAssignmentStatus] = {
    val timer = new Millitimer
    val exs = acquireExamples(w)
    exs.flatMap { pack =>
      logger.trace(s"setting ${w.writing.stris} <- ${pack.sentences.size}: ${pack.sentences.headOption.map(_.units.map(_.content).mkString)}")
      val uc = ucs.of(w.user.get) //keeping user ctx alive
      uc.inst[WordOps].setRepExamples(w.id.get, pack)
    }.map(_ => ExampleAssignmentStatus(w.user.get, w.id.get, 1, timer.eplaced))
  }

  def exampleRequest(w: WordRecord): ExamplePackRequest = {
    w.jmdictLink.get.flatMap(jmd.byId).orElse(wjo.entryForWord(w)) match {
      case Some(je) => WordExampleOps.jmdictReq(je)
      case None => WordExampleOps.wordRecordReq(w)
    }
  }

  def acquireExamples(w: WordRecord): Future[ExamplePack] = {
    val req = exampleRequest(w)
    val res = ec.generic(req)
    res
  }
}

object WordExampleOps {
  def wordRecordReq(rec: WordRecord): ExamplePackRequest = {
    val q = ExampleQuery(rec.writing.value.head, rec.reading.value.head)

    val tgs = rec.tags.get match {
      case t if t.contains("noun") && t.contains("vs") => ExampleTag.SuruVerb
      case t if t.contains("noun") => ExampleTag.Noun
      case t if t.contains("verb") => ExampleTag.Verb
      case t if t.contains("adj") => ExampleTag.Adjective
      case t if t.contains("adv") => ExampleTag.Adverb
      case _ => ExampleTag.Unknown
    }

    ExamplePackRequest(
      tags = Seq(tgs),
      candidates = Seq(q),
      limit = 15
    )
  }


  def jmdictReq(e: JmdictEntry) = {
    val qs = e.readings.flatMap { r =>
      val matchingWrs = if (r.restr.isEmpty) {
        e.writings
      } else {
        r.restr.map(e.writings.apply)
      }
      matchingWrs.map { w =>
        ExampleQuery(w.content, r.content) -> (JMDictUtil.calculatePriority(r) + JMDictUtil.calculatePriority(w))
      }
    }

    val qentries = qs.sortBy(-_._2).map(_._1)
    val qtags = new ArrayBuffer[ExampleTag]()

    val allTags = e.meanings.flatMap(_.pos).toSet

    if (allTags.contains(JmdictTag.vs)) {
      qtags += ExampleTag.SuruVerb
    }

    if (allTags.contains(JmdictTag.n)) {
      qtags += ExampleTag.Noun
    }

    if (allTags.intersect(JMDictUtil.verbTags).nonEmpty) {
      qtags += ExampleTag.Verb
    }

    if (allTags.intersect(JMDictUtil.adjTags).nonEmpty) {
      qtags += ExampleTag.Adjective
    }

    if (allTags.intersect(JMDictUtil.advTags).nonEmpty) {
      qtags += ExampleTag.Adverb
    }

    if (allTags.isEmpty) {
      qtags += ExampleTag.Unknown
    }

    ExamplePackRequest(
      qtags, qentries, limit = 15
    )
  }
}
