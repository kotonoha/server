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

import net.liftweb.util.Helpers
import ws.kotonoha.server.actors.ioc.{Akka, ReleaseAkka}
import ws.kotonoha.server.util.Formatting
import xml.{Text, NodeSeq}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds.FadeOut
import scala.concurrent.Await
import net.liftweb.http.SHtml
import net.liftweb.json
import net.liftweb.json.{DefaultFormats, Extraction}
import ws.kotonoha.server.records.{QrEntry, UserTokenRecord, UserRecord}
import net.liftweb.http.js.JsCmds.SetHtml
import ws.kotonoha.server.actors.{CreateQrWithLifetime, CreateToken}


/**
 * @author eiennohito
 * @since 01.04.12
 */

trait UserToken extends Akka {

  import Helpers._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._
  import ws.kotonoha.server.actors.UserSupport._

  implicit val formats = DefaultFormats
  implicit val ec = akkaServ.context


  def create(in: NodeSeq): NodeSeq = {
    var name: String = ""
    val uid = UserRecord.currentId.openOrThrowException("should have user here")

    def save(): JsCmd = {
      val fut = (akkaServ ? CreateToken(uid, name).forUser(uid)).mapTo[UserTokenRecord]
      val qrFut = fut flatMap {
        x =>
          val authStr = json.pretty(json.render(Extraction.decompose(x.auth)))
          (akkaServ ? CreateQrWithLifetime(authStr, 1 minute).forUser(uid)).mapTo[QrEntry]
      }
      val qr = Await.result(qrFut, 5 seconds)
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

    bind("tf", SHtml.ajaxForm(in),
      "label" -> SHtml.text(name, name = _),
      "create" -> SHtml.ajaxSubmit("Authorize client", save))
  }

  def list(in: NodeSeq): NodeSeq = {
    val uid = UserRecord.currentId.openOrThrowException("should have user here")
    val tokens = UserTokenRecord where (_.user eqs uid) fetch()

    def delete(t: UserTokenRecord): JsCmd = {
      t.delete_!
      FadeOut(t.hashCode().toString, 0 millis, 250 millis)
    }

    tokens flatMap {
      t =>
        bind("t", in,
          AttrBindParam("id", t.hashCode().toString, "id"),
          "name" -> t.label.get,
          "date" -> Formatting.format(t.createdOn.get),
          "delete" -> SHtml.a(() => delete(t), Text("delete"))
        )
    }
  }

}


object UserToken extends UserToken with ReleaseAkka
