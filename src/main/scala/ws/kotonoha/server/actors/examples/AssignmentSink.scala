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

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import com.google.inject.{Inject, Provides, Singleton}
import com.google.inject.name.Named
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import net.codingwell.scalaguice.ScalaModule
import org.bson.types.ObjectId
import ws.kotonoha.akane.akka.{MaxAtOnceActor, RateLimitCfg}
import ws.kotonoha.server.ops.WordExampleOps
import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.util.{GlobalRateLimiting, RateLimiter}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/22
  */

class AssignmentContext {

}

case class AssignExamplesRequest(uid: ObjectId)

class ExampleAssignment @Inject() (
  cfg: AssignExamplesConfig,
  wex: WordExampleOps,
  @Named("assign-examples")
  limiter: RateLimiter
)(implicit ec: ExecutionContext) extends StrictLogging {
  def wordsWithoutExamples(req: AssignExamplesRequest): Source[WordRecord, NotUsed] = {
    wex.wordsForAssign(req.uid)
  }

  def assignExamples(w: WordRecord): Future[Done] = wex.findAndAssign(w).recover {
    case t =>
      logger.error(s"could not fetch examples for word=${w.id.get} : ${w.reading.stris}", t)
      Done
  }

  def mainFlow = {
    val pure = Flow[WordRecord].mapAsyncUnordered(cfg.usersBatchSize)(assignExamples)
    val limited = limiter.limit(pure)

    Flow[AssignExamplesRequest]
      .flatMapMerge(cfg.usersBatchSize, wordsWithoutExamples)
      .via(limited)
  }
}



case class AssignExamplesConfig(
  usersBatchSize: Int,
  wordBatchSize: Int,
  concurrency: Int,
  fetchTimeout: FiniteDuration
)

class AssignExamplesModule extends ScalaModule {
  import ws.kotonoha.akane.config.ScalaConfig._
  override def configure() = {}

  @Provides
  @Singleton
  def cfg(cfg: Config): AssignExamplesConfig = {
    val icfg = cfg.getConfig("examples")
    val ubs = icfg.getInt("users-batch-size")
    val wbs = icfg.getInt("users-batch-size")
    val conc = icfg.getInt("concurrency")
    val ftm = icfg.finiteDuration("fetch-timeout")
    AssignExamplesConfig(
      ubs, wbs, conc, ftm
    )
  }

  @Provides
  @Named("assign-examples")
  @Singleton
  def limiter(
    cfg: AssignExamplesConfig,
    asys: ActorSystem
  )(implicit ec: ExecutionContext): RateLimiter = {
    val rlc = RateLimitCfg(cfg.concurrency, cfg.fetchTimeout)
    val props = MaxAtOnceActor.props(rlc)
    val aref = asys.actorOf(props, "aex-limiter")
    GlobalRateLimiting.forActor(aref)
  }
}
