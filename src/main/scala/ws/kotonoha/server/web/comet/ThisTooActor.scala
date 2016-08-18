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

package ws.kotonoha.server.web.comet

import com.google.inject.Inject
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.common.{Box, Full}
import net.liftweb.http.js.{JE, JsExp}
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST.{JField, JObject, JString}
import ws.kotonoha.lift.json.JLift
import ws.kotonoha.server.actors.AkkaMain
import ws.kotonoha.server.actors.lift.pertab.NamedMessageComet
import ws.kotonoha.server.actors.model.{Candidate, PresentStatus}
import ws.kotonoha.server.ops.SimilarWordOps

import scala.concurrent.ExecutionContext

/**
 * @author eiennohito
 * @since 13.03.13 
 */

case class ProcessThisToo(candidate: Candidate, id: String, src: String)

case class ThisTooUpdate(ptt: ProcessThisToo, ps: PresentStatus)

class ThisTooActor @Inject() (
  swo: SimilarWordOps,
  val akkaServ: AkkaMain
)(implicit val ec: ExecutionContext) extends NamedMessageComet with Logging {

  implicit val formats = DefaultFormats

  import JsExp._
  def thisToo(ptt: ProcessThisToo): Unit = {
    val cand = ptt.candidate
    swo.similarRegistered(cand).map(x => this ! ThisTooUpdate(ptt, x)).onFailure {
      case t => logger.error(s"error when processing $ptt", t)
    }
  }

  def replyToClient(tu: ThisTooUpdate): Unit = {
    val ps = tu.ps
    logger.info(s"we have match: ${ps.fullMatch} -> $tu")
    if (!ps.fullMatch) {
      val ptt = tu.ptt
      val part = JObject(JField("source", JString(ptt.src)) :: Nil)
      val jv = JLift.write(ps.cand).merge(part)
      partialUpdate(JE.Call("resolve_thistoo", JE.Str(ptt.id), jv).cmd)
    }
  }

  override def lowPriority = {
    case o: ProcessThisToo => thisToo(o)
    case ps: ThisTooUpdate => replyToClient(ps)
  }

  import net.liftweb.util.Helpers._

  override def lifespan: Box[TimeSpan] = Full(10.seconds)
}



