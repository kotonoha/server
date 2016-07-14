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
import net.liftweb.util.Helpers
import net.liftweb.http.SHtml
import ws.kotonoha.server.records.ClientRecord
import ws.kotonoha.server.actors.ioc.{ReleaseAkka, Akka}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._Noop
import scala.concurrent.Await
import ws.kotonoha.server.util.DateTimeUtils._
import ws.kotonoha.server.actors.auth.AddClient

/**
 * @author eiennohito
 * @since 25.03.12
 */

trait Clients extends Akka {
  def form(in: NodeSeq): NodeSeq = {
    import Helpers._
    val obj = ClientRecord.createRecord

    def onSave() = {
      val o = obj
      Await.ready(akkaServ ? AddClient(obj), 1 second)
    }

    bind("cf", in,
      "title" -> obj.name.toForm,
      "submit" -> SHtml.submit ("Save", onSave))
  }

  def list(in: NodeSeq) : NodeSeq = {
    import Helpers._
    val objs = ClientRecord.findAll

    def onDelete(rec: ClientRecord): JsCmd = {
      _Noop
    }

    objs.flatMap( o =>
      bind("cl", in,
        "title" -> o.name.get,
        "private" -> o.apiPrivate.get,
        "public" -> o.apiPublic.get,
        "delete" -> SHtml.button("delete", () => onDelete(o))
      )
    )
  }
}

object Clients extends Clients with ReleaseAkka
