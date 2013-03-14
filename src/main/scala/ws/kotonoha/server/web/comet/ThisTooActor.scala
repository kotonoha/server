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
import ws.kotonoha.server.actors.lift.AkkaInterop
import com.typesafe.scalalogging.slf4j.Logging
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.js.{JsExp, JE}
import net.liftweb.json.{Extraction, DefaultFormats}
import ws.kotonoha.server.records.{WordRecord, UserRecord}
import org.bson.types.ObjectId
import ws.kotonoha.server.records.events.AddWordRecord

/**
 * @author eiennohito
 * @since 13.03.13 
 */

case class ProcessThisToo(c: Candidate, id: String)

class ThisTooActor extends NamedCometActor with AkkaInterop with Logging with ReleaseAkka {
  import com.foursquare.rogue.LiftRogue._

  def render = <head_merge>
    <lift:cpres.js src="tools/this_too"></lift:cpres.js>
  </head_merge>

  lazy val userId = UserRecord.currentId

  def noWords(id: ObjectId, c: Candidate) = {
    val q = WordRecord where (_.user eqs id) and (_.writing eqs c.writing)
    val cnt = q.andOpt(c.reading)((a, b) => a.reading eqs (b)) count()
    cnt == 0
  }

  def noAdds(uid: ObjectId, c: Candidate) = {
    val q = AddWordRecord where (_.user eqs uid) and (_.writing eqs c.writing)
    val cnt = q.andOpt(c.reading)((m,r) => m.reading eqs r) count()
    cnt == 0
  }

  def thisToo(cand: Candidate, id: String, uid: ObjectId): Unit = {
    import JsExp._
    implicit val formats = DefaultFormats
    if (noWords(uid, cand) && noAdds(uid, cand)) {
      val jv = Extraction.decompose(cand)
      partialUpdate(JE.Call("resolve_thistoo", JE.Str(id), jv).cmd)
    }
  }

  override def lowPriority = {
    case ProcessThisToo(c, id) => if (userId.isDefined) thisToo(c, id, userId.get)
  }
}


