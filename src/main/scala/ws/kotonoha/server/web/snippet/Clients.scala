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

import akka.util.Timeout
import com.google.inject.Inject
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._Noop
import net.liftweb.http.js.{JsCmd, JsCmds}
import ws.kotonoha.server.actors.GlobalActors
import ws.kotonoha.server.actors.auth.AddClient
import ws.kotonoha.server.ioc.DateFormatting
import ws.kotonoha.server.records.ClientRecord

import scala.concurrent.Await
import scala.xml.NodeSeq

/**
 * @author eiennohito
 * @since 25.03.12
 */



class Clients @Inject() (
  ga: GlobalActors
)(implicit fmt: DateFormatting) {
  import ws.kotonoha.server.web.lift.Binders._

  def form(in: NodeSeq): NodeSeq = {
    val obj = ClientRecord.createRecord

    def onSave() = {
      val o = obj
      import akka.pattern.ask

      import scala.concurrent.duration._
      implicit val timeout: Timeout = 1.second
      Await.result(ga.global ? AddClient(obj), timeout.duration)
      JsCmds.Noop
    }

    val tf =
      ";title *+" #> obj.name.toForm &
      ";submit *+" #> SHtml.submit("Save", () => onSave(), "class" -> "btn")

    tf.apply(in)
  }

  def list(in: NodeSeq) : NodeSeq = {

    def onDelete(rec: ClientRecord): JsCmd = {
      rec.status(1).save()
      _Noop
    }

    val objs = ClientRecord.findAll

    val fn = objs.map { o =>
      ";created *+" #> o.registeredDate &
      ";title *+ " #> o.name &
      ";private *+" #> o.apiPrivate &
      ";public *+" #> o.apiPublic &
      ";delete *+" #> SHtml.ajaxButton("delete", () => onDelete(o))
    }

    ("*" #> fn).apply(in)
  }
}
