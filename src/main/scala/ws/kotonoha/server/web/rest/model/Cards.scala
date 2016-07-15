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

package ws.kotonoha.server.web.rest.model

import ws.kotonoha.server.web.rest.KotonohaRest
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.common.Full
import ws.kotonoha.server.model.learning.WordCard
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.util.unapply.{XInt, XOid}
import net.liftweb.http.{BadResponse, InMemoryResponse, JsonResponse}
import net.liftweb.json.JsonAST.JArray
import ws.kotonoha.server.records.WordCardRecord

/**
 * @author eiennohito
 * @since 13.04.13 
 */

object Cards extends KotonohaRest with ReleaseAkka {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  serve ("api" / "model" / "cards" prefix {
    case XOid(cid) :: Nil Get req =>
      val box = userId.flatMap { uid =>
        WordCardRecord where (_.id eqs cid) and (_.user eqs uid) get()
      } map { w => JsonResponse(w.asJValue) }
      Full(box openOr(BadResponse()))
    case XOid(cid) :: "marks" :: Nil Get req =>
      val uid = userId
      val limit = req.param("limit").flatMap(XInt.unapply).openOr(15)
      uid match {
        case Full(user) =>
          val marks = MarkEventRecord where (_.card eqs cid) and
            (_.user eqs user) orderDesc(_.datetime) fetch(limit)
          Full(JsonResponse(JArray(marks.map(_.asJValue))))
        case _ => BadResponse()
      }
  })
}
