/*
 * Copyright 2012-2013 eiennohito
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

import com.fmpwizard.cometactor.pertab.namedactor.InsertNamedComet
import scala.xml.NodeSeq
import net.liftweb.http.S
import ws.kotonoha.server.web.comet.{ProcessThisToo, Candidate}
import ws.kotonoha.server.records.UserRecord
import net.liftweb.common.Full
import net.liftweb.util.Helpers

/**
 * @author eiennohito
 * @since 13.03.13 
 */

object ThisToo {
  def A = ThisTooActorSnippet

  def makeNodeSeq(cand: Candidate): (NodeSeq, String) = {
    val id = Helpers.nextFuncName
    val ns = <span id={id} class="this-too-btn"></span>
    (ns, id)
  }

  def render(in: NodeSeq): NodeSeq = {
    UserRecord.currentUserId match {
      case Full(_) =>
        val rd: Option[String] = S.attr("rd")
        val wr = S.attr("wr") openOr("")
        val c = Candidate(wr, rd, None)
        val s = S.attr("src") openOr("this-too")
        val (ns, id) = makeNodeSeq(c)

        S.session.foreach(
          _.sendCometActorMessage(A.cometClass, Full(A.name),
            ProcessThisToo(c, id, s)
          )
        )

        ns
      case _ => NodeSeq.Empty
    }
  }
}

object ThisTooActorSnippet extends InsertNamedComet {
  def cometClass = "ThisTooActor"

  override def enabled = UserRecord.currentUserId.isDefined

  override def name = {
    val path = S.request.map(_.path.wholePath.mkString("_")).openOr("dummy")
    val user = UserRecord.currentUserId.openOr("nouser")
    s"thistoo_${user}_$path"
  }
}
