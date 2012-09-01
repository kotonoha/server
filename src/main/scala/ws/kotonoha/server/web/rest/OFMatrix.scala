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
import ws.kotonoha.server.records.{OFElementRecord, OFMatrixRecord, UserRecord}
import net.liftweb.json.JsonAST.JArray
import net.liftweb.http.JsonResponse

/**
 * @author eiennohito
 * @since 01.09.12
 */

object OFMatrix extends KotonohaRest with ReleaseAkka {
  import com.foursquare.rogue.Rogue._
  import ws.kotonoha.server.util.KBsonDSL._
  serve("api" / "ofmatrix" prefix {
    case Nil Get req => {
      val user = UserRecord.currentId
      user map (id => {
        val mid = OFMatrixRecord.forUser(id).id.is
        val items = OFElementRecord where (_.matrix eqs mid) fetch()
        val data = items map { i => ("ef" -> i.ef) ~ ("n" -> i.n) ~ ("val" -> i.value) }
        JsonResponse(JArray(data))
      })
    }
  })
}
