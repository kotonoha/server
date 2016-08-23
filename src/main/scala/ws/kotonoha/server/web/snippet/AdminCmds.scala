/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

import javax.inject.Inject

import akka.actor.ActorSystem
import net.liftweb.http.SHtml
import net.liftweb.http.js.{JsCmds, JsExp}
import ws.kotonoha.server.actors.GlobalActors
import ws.kotonoha.server.actors.examples.AssignExamplesActor

import scala.xml.NodeSeq

/**
  * @author eiennohito
  * @since 2016/08/23
  */
class AdminCmds @Inject() (
  gact: GlobalActors,
  asys: ActorSystem
) {
  def updateWords(in: NodeSeq): NodeSeq = {
    SHtml.ajaxButton("update words", () => {
      val path = gact.services.path / "aex"
      asys.actorSelection(path) ! AssignExamplesActor.DoAssign
      JsCmds.Noop
    })
  }
}
