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

import com.google.inject.Inject
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.jquery.JqJsCmds.FadeOut
import net.liftweb.json.{DefaultFormats, Extraction}
import net.liftweb.util.Helpers
import ws.kotonoha.server.actors.{CreateQrWithLifetime, CreateToken}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.records.{QrEntry, UserRecord, UserTokenRecord}
import ws.kotonoha.server.util.{Formatting, Json}

import scala.concurrent.{Await, ExecutionContext}
import scala.xml.{NodeSeq, Text}


/**
 * @author eiennohito
 * @since 01.04.12
 */

class UserToken @Inject() (
  ux: UserContext
)(
  implicit ec: ExecutionContext
) {

  import Helpers._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  implicit val formats = DefaultFormats

  def create(in: NodeSeq): NodeSeq = {
    var name: String = ""

    def save(): JsCmd = {
      val fut = ux.askUser[UserTokenRecord](CreateToken(ux.uid, name))
      val qrFut = fut flatMap {
        x =>
          val authStr = Json.str(Extraction.decompose(x.auth))
          ux.askUser[QrEntry](CreateQrWithLifetime(authStr, 1.minute))
      }
      val qr = Await.result(qrFut, 5.seconds)
      val code = qr.id.get.toString
      val uri = "/iqr/" + code
      SetHtml("qrcode",
        <span>
          Scan this QR Code to login
          <br/>
          <img src={uri}></img>
        </span>
      )
    }

    val fn =
      ";name *+" #> SHtml.text(name, name = _) &
      ";create-btn *+" #> SHtml.ajaxSubmit("Authorize client", save, "class" -> "btn")

    SHtml.ajaxForm(fn(in))
  }

  def list(in: NodeSeq): NodeSeq = {
    val uid = UserRecord.currentId.openOrThrowException("should have user here")
    val tokens = UserTokenRecord where (_.user eqs uid) fetch()

    def delete(t: UserTokenRecord): JsCmd = {
      t.delete_!
      FadeOut(t.hashCode().toString, 0 millis, 250 millis)
    }

    val f = tokens map { t =>
      "tr [id]" #> t.hashCode().toString &
      ";name *+" #> t.label.get &
      ";date *+" #> Formatting.format(t.createdOn.get) &
      ";delete *+" #> SHtml.a(() => delete(t), Text("delete"))
    }

    ("^" #> f).apply(in)
  }

}
