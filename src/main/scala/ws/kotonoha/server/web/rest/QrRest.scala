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

import ws.kotonoha.server.qr.QrRenderer
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import org.apache.commons.io.IOUtils
import net.liftweb.http.{LiftRules, InMemoryResponse, OutputStreamResponse}
import ws.kotonoha.server.records.QrEntry
import ws.kotonoha.server.util.unapply.XOid
import net.liftweb.common.Box


/**
 * @author eiennohito
 * @since 22.03.12
 */


trait QrRest extends KotonohaRest {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  lazy val invalidQr = LiftRules.doWithResource("/images/invalid_qr.png") {
          IOUtils.toByteArray(_)
        } ?~ ("Can't find invalid qr")

  serve {
    case "qr" :: code :: Nil Get req => {
      val renderer = new QrRenderer(code)
      OutputStreamResponse(s => renderer.toStream(s), -1, List("Content-Type" -> "image/png"))
    }

    case "iqr" :: XOid(code) :: Nil Get req => {
      val entry: Box[QrEntry] = QrEntry where (_.id eqs code) get()
      val arr = entry.map(_.binary.is).or(invalidQr)
      arr map (InMemoryResponse(_, List("Content-Type" -> "image/png"), Nil, 200))
    }
  }

}


object QrRest extends QrRest with ReleaseAkka
