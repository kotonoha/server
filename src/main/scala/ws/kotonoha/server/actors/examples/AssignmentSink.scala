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

package ws.kotonoha.server.actors.examples

import akka.actor.{ActorRef, ActorSystem, Scheduler}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import com.google.inject.{Inject, Provides, Singleton}
import com.google.inject.name.Named
import com.typesafe.config.Config
import com.typesafe.scalalogging.{Logger, StrictLogging}
import net.codingwell.scalaguice.ScalaModule
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import ws.kotonoha.akane.akka.{MaxAtOnceActor, RateLimitCfg, RateLimitTracing}
import ws.kotonoha.akane.utils.timers.Millis
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.ops.WordExampleOps
import ws.kotonoha.server.records.{UserRecord, UserStatus, WordRecord}
import ws.kotonoha.server.util.{GlobalRateLimiting, RateLimiter}
import ws.kotonoha.server.web.comet.TimeoutException

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * @author eiennohito
  * @since 2016/08/22
  */

class AssignmentContext {

}

case class AssignExamplesRequest(uid: ObjectId)

case class ExampleAssignmentStatus(uid: ObjectId, wid: ObjectId, number: Int, duration: Millis)

class ExampleAssignment @Inject() (
  cfg: AssignExamplesConfig,
  wex: WordExampleOps,
  @Named("assign-examples")
  limiter: RateLimiter,
  rd: RMData,
  sch: Scheduler
)(implicit ec: ExecutionContext) extends StrictLogging {

  def input: Source[AssignExamplesRequest, NotUsed] = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    val q = UserRecord.where(_.status eqs UserStatus.Active).orderAsc(_.id).select(_.id)
    rd.stream(q).map(id => AssignExamplesRequest(id))
  }

  def wordsWithoutExamples(req: AssignExamplesRequest): Source[WordRecord, NotUsed] = {
    wex.wordsForAssign(req.uid)
  }


  def assignExamples(w: WordRecord): Future[ExampleAssignmentStatus] = {
    val p = Promise[Nothing]
    val cancel = sch.scheduleOnce(cfg.fetchTimeout)(p.failure(new TimeoutException))
    Future.firstCompletedOf(Seq(wex.findAndAssign(w), p.future)).map {
      x =>
        cancel.cancel()
        x
    }.recover {
      case _: TimeoutException =>
        logger.warn(s"timeout for ${w.id.get}: ${w.writing.stris}")
        ExampleAssignmentStatus(w.user.get, w.id.get, 0, Millis(0L))
      case t =>
        logger.error(s"could not fetch examples for word=${w.id.get}: ${w.writing.stris}", t)
        ExampleAssignmentStatus(w.user.get, w.id.get, 0, Millis(0L))
    }
  }

  def mainFlow: Flow[AssignExamplesRequest, ExampleAssignmentStatus, NotUsed] = {
    val pure = Flow[WordRecord].mapAsyncUnordered(cfg.concurrency)(assignExamples)
    val limited = pure

    Flow[AssignExamplesRequest]
      .flatMapMerge(cfg.usersBatchSize, wordsWithoutExamples)
      .via(limited)
  }
}


case class AssignExamplesConfig(
  enabled: Boolean,
  usersBatchSize: Int,
  wordBatchSize: Int,
  concurrency: Int,
  fetchTimeout: FiniteDuration
)

class AssignExamplesModule extends ScalaModule with StrictLogging {
  import ws.kotonoha.akane.config.ScalaConfig._
  override def configure() = {}

  @Provides
  @Singleton
  def cfg(cfg: Config): AssignExamplesConfig = {
    val icfg = cfg.getConfig("examples")
    val enab = icfg.optBool("enabled").getOrElse(false)
    val ubs = icfg.getInt("users-batch-size")
    val wbs = icfg.getInt("words-batch-size")
    val conc = icfg.getInt("concurrency")
    val ftm = icfg.finiteDuration("fetch-timeout")
    val cf = AssignExamplesConfig(
      enab, ubs, wbs, conc, ftm
    )
    logger.debug(s"$cf")
    cf
  }

  @Provides
  @Named("assign-examples")
  @Singleton
  def limiter(
    cfg: AssignExamplesConfig,
    asys: ActorSystem
  )(implicit ec: ExecutionContext): RateLimiter = {

    val tracing = new RateLimitTracing {
      private val logger = Logger(LoggerFactory.getLogger("ws.kotonoha.server.ops.WordExampleOps"))
      override def enqueue(ref: ActorRef, tag: Any): Unit = {
        val wr = tag.asInstanceOf[WordRecord]
        logger.trace(s"ENQ: $ref, ${wr.writing.stris}")
      }
      override def start(ref: ActorRef, tag: Any, time: Long): Unit = {
        val wr = tag.asInstanceOf[WordRecord]
        logger.trace(s"ST: $ref, ${wr.writing.stris}")
      }
      override def finish(ref: ActorRef, tag: Any, start: Long): Unit = {
        val wr = tag.asInstanceOf[WordRecord]
        logger.trace(s"FN: $ref, ${wr.writing.stris}")
      }
      override def timeout(ref: ActorRef, tag: Any, start: Long): Unit = {
        val wr = tag.asInstanceOf[WordRecord]
        logger.trace(s"TO: $ref, ${wr.writing.stris}")
      }
    }

    val rlc = RateLimitCfg(cfg.concurrency, cfg.fetchTimeout, tracing)
    val props = MaxAtOnceActor.props(rlc)
    val aref = asys.actorOf(props, "aex-limiter")
    GlobalRateLimiting.forActor(aref)
  }
}
