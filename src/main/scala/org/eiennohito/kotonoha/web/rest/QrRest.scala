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

import org.eiennohito.kotonoha.qr.QrRenderer
import org.eiennohito.kotonoha.actors.ioc.ReleaseAkka
import net.liftweb.util.BasicTypesHelpers.AsLong
import java.io.{File, FileInputStream, InputStream}
import org.apache.commons.io.IOUtils
import net.liftweb.http.{LiftRules, InMemoryResponse, StreamingResponse, OutputStreamResponse}


/**
 * @author eiennohito
 * @since 22.03.12
 */

object HexLong {
  import net.liftweb.util.ControlHelpers.tryo
  def unapply(s: String): Option[Long] = {
    tryo {java.lang.Long.parseLong(s, 16)}
  }
}

trait QrRest extends KotonohaRest {

  lazy val invalidQr = LiftRules.doWithResource("/images/invalid_qr.png") {
          IOUtils.toByteArray(_)
        }

  serve {
    case "qr" :: code :: Nil Get req => {
      val renderer = new QrRenderer(code)
      OutputStreamResponse(s => renderer.toStream(s), -1, List("Content-Type" -> "image/png"))
    }

    case "iqr" :: HexLong(code) :: Nil Get req => {
      val arr = invalidQr.openTheBox
      InMemoryResponse(arr, List("Content-Type" -> "image/png"), Nil, 200)
    }
  }

}


object QrRest extends QrRest with ReleaseAkka