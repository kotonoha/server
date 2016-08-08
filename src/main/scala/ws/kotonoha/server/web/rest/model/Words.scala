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

import net.liftweb.common.{Box, Failure, Full}
import net.liftweb.http.{OkResponse, _}
import net.liftweb.json.JsonAST._
import org.bson.types.ObjectId
import ws.kotonoha.model.WordStatus
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import ws.kotonoha.server.actors.model.{ChangeWordStatus, MarkForDeletion}
import ws.kotonoha.server.actors.tags.{TagParser, TagWord}
import ws.kotonoha.server.records.{UserRecord, WordCardRecord, WordRecord}
import ws.kotonoha.server.util.unapply.XOid
import ws.kotonoha.server.web.rest.KotonohaRest

import scala.concurrent.Future

/**
 * @author eiennohito
 * @since 02.09.12
 */

object Words extends KotonohaRest with ReleaseAkka {
  import ws.kotonoha.server.actors.UserSupport._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def updateWord(updated: JValue, user: ObjectId, wid: ObjectId): Future[Box[LiftResponse]] = {
    val rec = WordRecord where (_.id eqs (wid)) and (_.user eqs (user)) get()
    rec.fut flatMap (r => {
      val js = WordRecord.trimInternal(updated, out = false)
      val c = r.setFieldsFromJValue(js)
      //r.fields().map(_.validate)
      r.save
      val tags = updated \ "tags"
      val data = TagParser.parseOps(tags)
      val f = if (data.ops.nonEmpty)
        akkaServ ? TagWord(r, data.ops).u(user)
      else Future {
        null
      }
      f map {
        _ => Full(OkResponse())
      }
    })
  }

  serve("api" / "model" / "words" prefix {
    case XOid(id) :: Nil JsonGet req => {
      val uid = UserRecord.currentId
      val jv = uid.flatMap(user => {
        WordRecord where (_.id eqs (id)) and (_.user eqs (user)) get()
      }).map {
        _.stripped.map {
          case JField("status", JInt(v)) => JField("status", JString(WordStatus.fromValue(v.intValue()).name))
          case x => x
        }
      }
      jv.map {
        j => JsonResponse(j)
      } ~> (401)
    }
    case XOid(wid) :: Nil JsonPost reqV => {
      val (obj, req) = reqV
      val uid = UserRecord.currentId
      val res: Box[LiftResponse] = obj \ "command" match {
        case JString("update") => async(uid) {
          id => updateWord(obj \ "content", id, wid)
        }
        case JString("update-approve") => {
          async(uid)(id => {
            val succ = updateWord(obj \ "content", id, wid)
            succ.foreach {
              _ =>
                akkaServ ! ChangeWordStatus(wid, WordStatus.Approved).u(id)
            }
            succ
          })
        }
        case _ => Failure("Can't handle request") ~> 401
      }
      res
    }
    case XOid(wid) :: Nil Delete req => {
      val uid = UserRecord.currentId
      val id = uid.openOrThrowException("")
      val cnt = WordRecord where (_.id eqs wid) and (_.user eqs id) count()
      if (cnt == 1) {
        akkaServ ! MarkForDeletion(wid).u(id)
        OkResponse()
      } else {
        ForbiddenResponse()
      }
    }
    case XOid(wid) :: "cards" :: Nil Get req => {
      val cards = userId map { uid =>
        WordCardRecord where (_.word eqs wid) and (_.user eqs uid) fetch()
      } map { ws => JsonResponse(JArray(ws.map(_.asJValue)))}
      Full(cards.openOr(ForbiddenResponse()))
    }
  })
}
