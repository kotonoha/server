/*
 * Copyright 2012 eiennohito
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
import ws.kotonoha.server.util.unapply.XHexLong
import ws.kotonoha.server.records.{WordStatus, WordRecord, UserRecord}
import net.liftweb.http.{LiftResponse, OkResponse, ForbiddenResponse, JsonResponse}
import net.liftweb.common.{Failure, Box, Full}
import net.liftweb.json.JsonAST.{JInt, JField, JValue, JString}
import ws.kotonoha.server.actors.model.ChangeWordStatus
import ws.kotonoha.server.tools.JsonAstUtil

/**
 * @author eiennohito
 * @since 02.09.12
 */

object Words extends KotonohaRest with ReleaseAkka {
  import com.foursquare.rogue.Rogue._

  def updateWord(updated: JValue, user: Long, wid: Long): Box[LiftResponse] = {
    val rec = WordRecord where (_.id eqs (wid)) and (_.user eqs (user)) get()
    rec map (r => {
      val js = WordRecord.trimInternal(updated, out = false)
      val clean = JsonAstUtil.clean(js, saveArrays = true)
      val c = r.setFieldsFromJValue(clean)
      //r.fields().map(_.validate)
      r.save
      OkResponse()
    })
  }

  serve("api" / "model" / "words" prefix {
    case XHexLong(id) :: Nil JsonGet req => {
      val uid = UserRecord.currentId
      val jv = uid.flatMap(user => {
        WordRecord where (_.id eqs(id)) and (_.user eqs (user)) get()
      }).map {_.stripped.map {
        case JField("status", JInt(v)) => JField("status", JString(WordStatus(v.intValue()).toString))
        case x => x
      }}
      jv.map {j => JsonResponse(j)} ~> (401)
    }
    case XHexLong(wid) :: Nil JsonPost reqV => {
      val (obj, req) = reqV
      val uid = UserRecord.currentId
      val res = obj \ "command" match {
        case JString("update") => uid flatMap { id => updateWord(obj \ "content", id, wid) }
        case JString("update-approve") => {
          uid flatMap (id => {
            val succ = updateWord(obj \ "content", id, wid)
            if (!succ.isEmpty) {
              akkaServ ! ChangeWordStatus(wid, WordStatus.Approved)
            }
            succ
          })
        }
        case _ => Failure("Can't handle request")
      }
      (res ~> 401)
    }
  })
}
