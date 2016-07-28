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

package ws.kotonoha.server.actors.recommend

import ws.kotonoha.server.actors.{KotonohaMessage, UserScopedActor}
import ws.kotonoha.server.dict.{RecommendedSubresult, Recommender}
import akka.actor.ActorRef
import ws.kotonoha.server.actors.interop.ParseSentence
import akka.util.Timeout
import com.google.inject.{Inject, Provider}
import org.joda.time.DateTime
import ws.kotonoha.akane.pipe.juman.{JumanEntry, ParsedQuery}
import ws.kotonoha.dict.jmdict.{IdQueryPart, JmdictIdQuery, LuceneJmdict}
import ws.kotonoha.server.records.events.AddWordRecord
import ws.kotonoha.server.records.{RecommendationIgnore, WordRecord}

import scala.collection.mutable.ArrayBuffer

/**
  * @author eiennohito
  * @since 18.03.13
  */

trait RecommenderMessage extends KotonohaMessage

/**
  *
  * @param writ  Word writing
  * @param read  word reading
  * @param juman juman analyze results
  */
case class RecommendRequest(writ: Option[String], read: Option[String], juman: List[JumanEntry]) extends RecommenderMessage

case object LoadRecommender extends RecommenderMessage

case class InternalRequest(snd: ActorRef, c: RecommendRequest, juman: List[JumanEntry])

case class RecommenderReply(req: RecommendRequest, entries: List[RecommendedSubresult])

case object PingRecommender

class RecommendActor @Inject()(
  jmd: Provider[LuceneJmdict]
) extends UserScopedActor {

  import akka.pattern.{ask, pipe}
  import scala.concurrent.duration._

  private[this] var recFld: Recommender = null
  private[this] var recAccess: DateTime = DateTime.now()

  private[this] def rec(): Recommender = {
    recAccess = DateTime.now()
    if (recFld == null) {
      recFld = new Recommender(uid, jmd.get(), loadIgnores())
    }
    recFld
  }

  def resolveIgnores(): Seq[Long] = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

    val qrs = new ArrayBuffer[IdQueryPart]()

    val dbwrs = WordRecord where (_.user eqs uid) select(_.writing) foreach { l =>
      qrs += IdQueryPart(wrs = l, rds = Nil)
    }

    val addwrs = AddWordRecord where (_.user eqs uid) and (_.processed eqs false) select(_.writing) foreach { l =>
      qrs += IdQueryPart(wrs = Seq(l), rds = Nil)
    }

    jmd.get().ids(JmdictIdQuery(qrs))
  }

  def loadIgnores(): Seq[Long] = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    val ignores = RecommendationIgnore where (_.user eqs uid) select(_.jmdict) fetch()
    val saved = resolveIgnores()
    ignores ++ saved
  }

  def processRequest(who: ActorRef, req: RecommendRequest, juman: List[JumanEntry]): Unit = {
    val msg = req.copy(juman = juman)
    internalProcess(msg, who)
  }

  def internalProcess(msg: RecommendRequest, who: ActorRef) {
    val res = rec().process(msg)
    who ! RecommenderReply(msg, res)
  }

  def receive = {
    case LoadRecommender => rec()
    case PingRecommender =>
      if (recAccess.plusMinutes(15).isBefore(DateTime.now())) {
        recFld = null
      }
    case rr: RecommendRequest => processRequest(rr, sender())
    case InternalRequest(who, cand, juman) => processRequest(who, cand, juman)
  }

  val cancel = context.system.scheduler.schedule(5.minutes, 5.minutes, self, PingRecommender)

  override def postStop() = {
    cancel.cancel()
    super.postStop()
  }

  implicit val timeout: Timeout = 10 seconds

  def processRequest(rr: RecommendRequest, snd: ActorRef): Unit = {
    if (rr.juman.isEmpty && rr.writ.isDefined) {
      val f = (services ? ParseSentence(rr.writ.get)).mapTo[ParsedQuery]
      f.map(pq => InternalRequest(snd, rr, pq.inner)) pipeTo self
    } else {
      internalProcess(rr, snd)
    }
  }
}
