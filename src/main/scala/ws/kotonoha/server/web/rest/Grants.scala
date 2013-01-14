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

package ws.kotonoha.server.web.rest

import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.{OkResponse, JsonResponse}
import net.liftweb.common.Full
import ws.kotonoha.server.records.UserRecord
import ws.kotonoha.server.security.{Roles, GrantRecord}
import net.liftweb.json.JsonAST.JObject
import net.liftweb.mongodb.{Skip, Limit}
import ws.kotonoha.server.util.unapply.{XOid, XLong, XInt}
import ws.kotonoha.server.actors.GrantManager
import ws.kotonoha.server.util.{SecurityUtil, UserUtil}
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 19.08.12
 */

object Grants extends KotonohaRest with ReleaseAkka {
  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.KBsonDSL._

  def mergeRoles(user: ObjectId, roles: String): Unit = {
    val present =
      GrantRecord where (_.user eqs (user)) select (_.role) fetch() flatMap (Roles.safeRole(_)) toSet
    val got = roles split(",") map (_.trim) flatMap ( Roles.safeRole(_) ) toSet
    val added = got.diff(present)
    val removed = present.diff(got)
    added foreach (GrantManager.grantRole(user, _))
    removed foreach (GrantManager.revokeRole(user, _))
  }

  def createModFn(rec: UserRecord) = {
    val id = rec.id.is
    val key = UserUtil.serverKey
    val ids = id.toString
    SecurityUtil.encryptAes(ids, key)
  }

  serve("api"/ "sec" prefix {
    case "grants" :: Nil JsonGet req => {
      val srch = req.param("sSearch") openOr("")
      val re : JObject = ("$regex" -> ("^" + srch)) ~ ("$options" -> "i")

      val q: JObject = "$or" -> List(("email" -> re), ("firstName" -> re), ("lastName" -> re))

      val (start, limit) = (req.param("iDisplayStart"), req.param("iDisplayLength")) match {
        case (Full(XInt(st)), Full(XInt(len))) => (st, len)
        case _ => (0, 10)
      }
      val sort: JObject = "email" -> 1

      val count = UserRecord count
      val users = UserRecord findAll(q, sort, Skip(start), Limit(limit))
      val uids = users map (_.id.is)
      val grants = GrantRecord where (_.user in (uids)) fetch()

      def gnames(u: ObjectId) = grants filter { _.user.is == u } map {_.role.is} mkString(",")

      val list = users map (u => {
        ("name" -> u.shortName) ~ ("roles" -> gnames(u.id.is)) ~ ("code" -> createModFn(u))
      })
      val resp = ("sEcho" -> req.param("sEcho").openOr("1")) ~ ("iTotalRecords" -> count) ~
        ("iTotalDisplayRecords" -> users.length) ~ ("aaData" -> list)
      Full(JsonResponse(resp))
    }

    case "grant_role" :: Nil Post reqV => {
      val roles = reqV.param("roles")
      val code = reqV.param("code")
      (roles, code) match {
        case (Full(r), Full(c)) => {
          val key = UserUtil.serverKey
          val ids = SecurityUtil.decryptAes(c, key)
          XOid.unapply(ids) foreach (mergeRoles(_, r))
        }
        case _ => ///
      }
      OkResponse()
    }
  })
}
