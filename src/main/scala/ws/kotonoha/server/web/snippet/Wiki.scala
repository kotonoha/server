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

import ws.kotonoha.server.web.loc.WikiPage
import net.liftweb.http.{S, LiftSession}
import scala.xml.NodeSeq
import net.liftweb.sitemap.Loc.Snippet
import ws.kotonoha.server.records.{WikiPageRecord, UserRecord}
import ws.kotonoha.server.wiki.WikiRenderer
import net.liftweb.common.Full
import net.liftweb.util.Helpers
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author eiennohito
 * @since 16.04.13 
 */

class Wiki(page: WikiPage, sess: LiftSession) extends Logging {
  import com.foursquare.rogue.LiftRogue._

  private val loggedIn = UserRecord.currentUserId.isDefined

  def editUri = ("/wiki" :: page.path).mkString("/") + "?mode=edit"

  def historyUri = ("/wiki" :: page.path).mkString("/") + "?mode=history"

  def editPanel =
    <div class="pull-right">
      <a href={historyUri} class="btn" title="History"><i class="icon-time"></i> History</a>
      <a href={editUri} class="btn" title="Edit"><i class="icon-pencil"></i> Edit</a>
    </div>

  def empty = {
    logger.debug(s"my uri is $editUri")
    if (loggedIn)
      <p>This page doesn't exist yet. You can <a href={editUri}>create</a> it.</p>
    else <p>This page doesn't exist yet</p>
  }

  def renderMarkdown(db: WikiPageRecord): NodeSeq = {
    WikiRenderer.parseMarkdown(db.source.is, db.path.is)
  }

  def edit: NodeSeq = {
    val name = Helpers.nextFuncName
    if (loggedIn)
    {
      sess.sendCometActorMessage("EditWiki", Full(name), page)
      <lift:comet type="EditWiki" name={name}></lift:comet>
    }
    else Nil
  }

  def render(ns: NodeSeq): NodeSeq = {
    S.request.flatMap(_.param("mode")) match {
      case Full("edit") => edit
      case Full("history") => <p>Implement me</p> //implement history
      case _ => normal
    }
  }

  def normal: NodeSeq = {
    val head = if (loggedIn) editPanel else Nil
    val path = page.path.mkString("/")
    val rec = WikiPageRecord where (_.path eqs path) orderDesc (_.datetime) get()
    val body = if (rec.isDefined)
      renderMarkdown(rec.get)
    else
      empty
    head ++ body
  }
}

