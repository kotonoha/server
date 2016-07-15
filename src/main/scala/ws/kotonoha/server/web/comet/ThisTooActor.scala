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

import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import ws.kotonoha.server.actors.lift.{ToAkka, AkkaInterop}
import com.typesafe.scalalogging.{StrictLogging => Logging}
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.js.{JsExp, JE}
import net.liftweb.json.{Extraction, DefaultFormats}
import ws.kotonoha.server.records.{WordRecord, UserRecord}
import org.bson.types.ObjectId
import ws.kotonoha.server.records.events.AddWordRecord
import scala.collection.mutable
import ws.kotonoha.server.actors.model.{SimilarWordsRequest, PresentStatus}
import ws.kotonoha.server.actors.ForUser
import net.liftweb.json.JsonAST.{JString, JField, JObject}

/**
 * @author eiennohito
 * @since 13.03.13 
 */

case class ProcessThisToo(candidate: Candidate, id: String, src: String)

class ThisTooActor extends NamedCometActor with AkkaInterop with Logging with ReleaseAkka {
  def render = <head_merge>
    <lift:cpres.js src="tools/this_too"></lift:cpres.js>
  </head_merge>

  lazy val userId = UserRecord.currentId

  val storage = new mutable.HashMap[Candidate, ProcessThisToo]()

  implicit val formats = DefaultFormats

  import JsExp._
  def thisToo(ptt: ProcessThisToo, uid: ObjectId): Unit = {
    val cand = ptt.candidate
    toAkka(ForUser(uid, SimilarWordsRequest(cand)))
    storage.update(cand, ptt)
  }

  def replyToClient(ps: PresentStatus): Unit = {
    if (!ps.fullMatch) {
      storage.get(ps.cand) match {
        case Some(ptt) =>
          val cand = ps.cand
          val part = JObject(JField("source", JString(ptt.src)) :: Nil)
          val jv = Extraction.decompose(cand).merge(part)
          storage.remove(cand)
          partialUpdate(JE.Call("resolve_thistoo", JE.Str(ptt.id), jv).cmd)
        case _ =>
      }
    } else {
      storage.remove(ps.cand)
    }
  }

  override def lowPriority = {
    case o: ProcessThisToo => if (userId.isDefined) thisToo(o, userId.get)
    case ps: PresentStatus => replyToClient(ps)
  }
}



