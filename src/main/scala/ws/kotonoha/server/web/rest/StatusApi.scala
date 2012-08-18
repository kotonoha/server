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
import net.liftweb.common.Full
import net.liftweb.http.PlainTextResponse
import ws.kotonoha.server.records.{WordCardRecord, UserRecord}

/**
 * @author eiennohito
 * @since 01.04.12
 */

trait StatusTrait extends KotonohaRest with OauthRestHelper {
  import com.foursquare.rogue.Rogue._
  import ws.kotonoha.server.util.DateTimeUtils._
  serve {
    case "api" :: "status" :: Nil Get req => {
      userId match {
        case Full(id) => {
          val user = UserRecord.find(id).openTheBox
          val cards = WordCardRecord where (_.user eqs id) and (_.notBefore before now) and
            (_.learning subfield(_.intervalEnd) before now) count()
          PlainTextResponse("You are user " + user.username.is + " and have " + cards + " cards scheduled")
        }
        case _ => PlainTextResponse("Should not get this", 320)
      }
    }
  }
}

class StatusApi extends StatusTrait with ReleaseAkka
