/*
 * Copyright 2012-2013 eiennohito
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
import com.typesafe.scalalogging.slf4j.Logging
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.js.{JsExp, JE}
import net.liftweb.json.{Extraction, DefaultFormats}
import ws.kotonoha.server.records.{WordRecord, UserRecord}
import org.bson.types.ObjectId
import ws.kotonoha.server.records.events.AddWordRecord
import scala.collection.mutable
import ws.kotonoha.server.actors.model.{SimilarWordsRequest, PresentStatus}
import ws.kotonoha.server.actors.ForUser

/**
 * @author eiennohito
 * @since 13.03.13 
 */

case class ProcessThisToo(c: Candidate, id: String)

class ThisTooActor extends NamedCometActor with AkkaInterop with Logging with ReleaseAkka {
  def render = <head_merge>
    <lift:cpres.js src="tools/this_too"></lift:cpres.js>
  </head_merge>

  lazy val userId = UserRecord.currentId

  val storage = new mutable.HashMap[Candidate, String]()

  implicit val formats = DefaultFormats

  import JsExp._
  def thisToo(cand: Candidate, id: String, uid: ObjectId): Unit = {
    toAkka(ForUser(uid, SimilarWordsRequest(cand)))
    storage.update(cand, id)
  }

  def replyToClient(ps: PresentStatus): Unit = {
    if (!ps.fullMatch) {
      storage.get(ps.cand) match {
        case Some(id) =>
          val cand = ps.cand
          val jv = Extraction.decompose(cand)
          storage.remove(cand)
          partialUpdate(JE.Call("resolve_thistoo", JE.Str(id), jv).cmd)
        case _ =>
      }
    } else {
      storage.remove(ps.cand)
    }
  }

  override def lowPriority = {
    case ProcessThisToo(c, id) => if (userId.isDefined) thisToo(c, id, userId.get)
    case ps: PresentStatus => replyToClient(ps)
  }
}



