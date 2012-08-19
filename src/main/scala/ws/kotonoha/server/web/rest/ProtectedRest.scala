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

import ws.kotonoha.server.security.Roles
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.{ForbiddenResponse, Req}
import ws.kotonoha.server.records.UserRecord
import net.liftweb.common.{Full, Empty}
import ws.kotonoha.server.actors.GrantManager

/**
 * @author eiennohito
 * @since 19.08.12
 */

class ProtectedRest(role: Roles.Role) extends RestHelper {
  override def apply(in: Req) = {
    val user = UserRecord.currentId
    user match {
      case Full(u) => {
        if (GrantManager.checkRole(u, role)) {
          super.apply(in)
        } else {
          Full(ForbiddenResponse("you are not allowed here"))
        }
      }
      case _ => Full(ForbiddenResponse("you are not allowed here"))
    }
  }
}
