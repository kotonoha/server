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

package ws.kotonoha.server.web.snippet

import net.liftweb.http.S
import xml.NodeSeq
import ws.kotonoha.server.records.UserRecord
import net.liftweb.common.Full
import ws.kotonoha.server.security.Roles
import ws.kotonoha.server.actors.GrantManager

/**
 * @author eiennohito
 * @since 19.08.12
 */

object Security {
  import net.liftweb.util.Helpers._
  def withRole(in: NodeSeq): NodeSeq = {
    val user = UserRecord.currentId
    val roleName = S.attr("role")
    val role = roleName flatMap { n => tryo { Roles.withName(n) } }
    (role, user) match {
      case (Full(r), Full(u)) => {
        if (GrantManager.checkRole(u, r)) {
          in
        } else {
          Nil
        }
      }
      case (_, u) => {
        <b>Invalid role with name {roleName}, we are at {S.location}</b>
      }
      case _ => Nil
    }
  }
}
