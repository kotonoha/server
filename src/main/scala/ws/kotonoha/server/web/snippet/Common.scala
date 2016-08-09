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

import xml.{NodeSeq, Text}
import ws.kotonoha.server.util.Formatting
import org.joda.time.DateTime
import com.fmpwizard.cometactor.pertab.namedactor.InsertNamedComet
import ws.kotonoha.server.web.comet.ActorUser
import ws.kotonoha.server.records.UserRecord
import net.liftweb.http.{DispatchSnippet, S, SessionVar}

/**
 * @author eiennohito
 * @since 05.04.12
 */

object Common {

  def formatDate(l: Long) = {
    Formatting.format(new DateTime(l))
  }

  def versionInfo(in: NodeSeq) : NodeSeq = {
    val bi = ws.kotonoha.server.util.BuildInfo
    val gitId = bi.gitId.substring(0, 7)
    val version = bi.version
    val gitDate = bi.gitDate


    val link = <a href="https://github.com/kotonoha/server">Git revision {gitId}</a> ;
    <span>Version {version}, {link} on {formatDate(gitDate)}</span>
  }

  def year(in: NodeSeq): NodeSeq = {
    val year = DateTime.now().getYear.toString
    Text(year)
  }
}

object GlobalComet extends InsertNamedComet {
  object actorName extends SessionVar[String] (
    UserRecord.currentUserId map {u => s"global_$u" } openOr super.name
  )

  def cometClass = "GlobalActor"

  override def messages = UserRecord.currentId.map(p => ActorUser(p)).toList

  override def name = actorName.get

  override def enabled = S.statefulRequest_?
}

