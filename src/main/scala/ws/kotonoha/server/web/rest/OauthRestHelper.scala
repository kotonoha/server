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

import net.liftweb.http.rest.RestHelper
import net.liftweb.util.ThreadGlobal
import ws.kotonoha.server.records.{UserRecord, NonceRecord, ClientRecord, UserTokenRecord}
import net.liftmodules.oauth.OAuthUtil.Parameter
import net.liftweb.common.{Empty, Full, Box}
import net.liftmodules.oauth.{HttpRequestMessage, OAuthAccessor, OAuthMessage, OAuthValidator}
import net.liftweb.http._
import net.liftmodules.oauth.OAuthUtil.Parameter
import net.liftmodules.oauth.OAuthAccessor
import net.liftweb.common.Full

trait OauthRestHelper extends RestHelper {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  private object restUser extends ThreadGlobal[Box[UserTokenRecord]]
  private object restClient extends ThreadGlobal[Box[ClientRecord]]

  private def userId = restUser.value map (_.user.get)

  private val validator = new OAuthValidator {
    protected def oauthNonceMeta = NonceRecord
  }

  def process(key: Box[Parameter], token: Box[Parameter]): Boolean = {

    val user = token flatMap {
      t =>
        UserTokenRecord where (_.tokenPublic eqs t.value) get()
    }
    val client = key flatMap {
      t =>
        ClientRecord where (_.apiPublic eqs t.value) get()
    }

    restUser(user)
    restClient(client)

    !(restUser.value.isEmpty || restClient.value.isEmpty)
  }

  def check(msg: OAuthMessage): Boolean = {
    val validated = (restUser.value, restClient.value) match {
      case (Full(user), Full(client)) =>
        val assessor = new OAuthAccessor(client, Full(user.tokenSecret.get), Empty)
        validator.validateMessage(msg, assessor)
      case _ => Empty
    }

    !validated.isEmpty
  }

  def clientName: String = restClient.value.map(_.name.get).openOr("passthrough")

  def needAuth = true

  override def apply(in: Req) : () => Box[LiftResponse] = {
    if (in.header("Authorization").isEmpty && !needAuth) {
      return super.apply(in)
    }

    val msg = new HttpRequestMessage(in)
    val key = msg.getConsumerKey
    val tok = msg.getToken

    try {
      if (!process(key, tok) || !check(msg)) {
        () => Full(ForbiddenResponse("Invalid OAuth"))
      } else {
        val user = userId.flatMap(UserRecord.find(_))
        if (!S.inStatefulScope_?) {
          S.init(in, LiftSession.apply(in)) {
            UserRecord.doWithUser(user) {
              super.apply(in)
            }
          }
        } else {
          UserRecord.doWithUser(user) {
            super.apply(in)
          }
        }
      }
    } finally {
      restClient(Empty)
      restUser(Empty)
    }
  }
}
