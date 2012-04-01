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

package org.eiennohito.kotonoha.web.rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.util.ThreadGlobal
import org.eiennohito.kotonoha.records.{NonceRecord, ClientRecord, UserTokenRecord}
import net.liftweb.oauth.OAuthUtil.Parameter
import net.liftweb.common.{Empty, Full, Box}
import net.liftweb.oauth.{HttpRequestMessage, OAuthAccessor, OAuthMessage, OAuthValidator}
import net.liftweb.http.{LiftResponse, ForbiddenResponse, Req}

trait OauthRestHelper extends RestHelper {

  import com.foursquare.rogue.Rogue._

  object restUser extends ThreadGlobal[Box[UserTokenRecord]]
  object restClient extends ThreadGlobal[Box[ClientRecord]]

  def userId = restUser.value map (_.user.is)

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
    val user = restUser.value.openTheBox
    val client = restClient.value.openTheBox
    val assessor = new OAuthAccessor(client, Full(user.tokenSecret.is), Empty)
    val validated = validator.validateMessage(msg, assessor)
    !validated.isEmpty
  }

  override def apply(in: Req) : () => Box[LiftResponse] = {
    val msg = new HttpRequestMessage(in)
    val key = msg.getConsumerKey
    val tok = msg.getToken

    try {
      if (!process(key, tok) || !check(msg)) {
        () => Full(ForbiddenResponse("Invalid OAuth"))
      } else {
        super.apply(in)
      }
    } finally {
      restClient(Empty)
      restUser(Empty)
    }
  }
}
