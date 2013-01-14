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
import ws.kotonoha.server.util.unapply.{XOid, XHexLong}
import ws.kotonoha.server.records.{WordStatus, WordRecord, UserRecord}
import net.liftweb.http._
import net.liftweb.common.{Failure, Box, Full}
import net.liftweb.json.JsonAST.{JInt, JField, JValue, JString}
import ws.kotonoha.server.actors.model.{MarkForDeletion, ChangeWordStatus}
import ws.kotonoha.server.tools.JsonAstUtil
import ws.kotonoha.server.actors.model.ChangeWordStatus
import net.liftweb.http.OkResponse
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JString
import net.liftweb.json.JsonAST.JInt
import ws.kotonoha.server.actors.model.MarkForDeletion
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 02.09.12
 */

object Words extends KotonohaRest with ReleaseAkka {
  import com.foursquare.rogue.LiftRogue._

  def updateWord(updated: JValue, user: ObjectId, wid: ObjectId): Box[LiftResponse] = {
    val rec = WordRecord where (_.id eqs (wid)) and (_.user eqs (user)) get()
    rec map (r => {
      val js = WordRecord.trimInternal(updated, out = false)
      val c = r.setFieldsFromJValue(js)
      //r.fields().map(_.validate)
      r.save
      OkResponse()
    })
  }

  serve("api" / "model" / "words" prefix {
    case XOid(id) :: Nil JsonGet req => {
      val uid = UserRecord.currentId
      val jv = uid.flatMap(user => {
        WordRecord where (_.id eqs(id)) and (_.user eqs (user)) get()
      }).map {_.stripped.map {
        case JField("status", JInt(v)) => JField("status", JString(WordStatus(v.intValue()).toString))
        case x => x
      }}
      jv.map {j => JsonResponse(j)} ~> (401)
    }
    case XOid(wid) :: Nil JsonPost reqV => {
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
    case XOid(wid) :: Nil Delete req => {
      val uid = UserRecord.currentId
      val cnt = WordRecord where (_.id eqs wid) and (_.user eqs uid.openOrThrowException("")) count()
      if (cnt == 1) {
        akkaServ ! MarkForDeletion(wid)
        OkResponse()
      } else {
        ForbiddenResponse()
      }
    }
  })
}
