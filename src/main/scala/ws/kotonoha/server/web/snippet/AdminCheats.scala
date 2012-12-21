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

import xml.NodeSeq
import net.liftweb.http.SHtml
import ws.kotonoha.server.util.unapply.{XOid, XLong}
import ws.kotonoha.server.records.UserRecord

/**
 * @author eiennohito
 * @since 20.07.12
 */

object AdminCheats {
  import net.liftweb.util.BindHelpers._
  def impersonForm(in: NodeSeq) = {

    def changeId(s: String) = {
      s.trim match {
        case XOid(id) => UserRecord.logUserIdIn(id.toString)
        case _ => //
      }
    }

    val item = SHtml.text("", changeId)

    <form action="debug">
      <b>User Id</b>
      {item}
      <input type="submit"></input>
    </form>
  }

  def userList(in: NodeSeq) = {
    val users = UserRecord.findAll
    users flatMap {
      u => bind("c", in,
            "name" -> u.niceName,
            "uid" -> u.id.is.toString )
    }

  }
}
