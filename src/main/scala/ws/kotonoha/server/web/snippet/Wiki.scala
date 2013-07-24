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
import net.liftweb.http.{Templates, S, LiftSession}
import scala.xml.NodeSeq
import net.liftweb.sitemap.Loc.Snippet
import ws.kotonoha.server.records.{WikiPageRecord, UserRecord}
import ws.kotonoha.server.wiki.WikiRenderer
import net.liftweb.common.{Empty, Full}
import net.liftweb.util.Helpers
import com.typesafe.scalalogging.slf4j.Logging
import org.bson.types.ObjectId
import org.joda.time.DateTime
import ws.kotonoha.server.util.unapply.XOid
import ws.kotonoha.server.util.Formatting

/**
 * @author eiennohito
 * @since 16.04.13 
 */

case class WikiHistoryItem(id: ObjectId, user: String, date: DateTime, comment: String, size: Long)

class Wiki(page: WikiPage, sess: LiftSession) extends Logging {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  private val loggedIn = UserRecord.currentUserId.isDefined

  lazy val path = page.path.mkString("/")

  lazy val mainPath: String = ("/wiki" :: page.path).mkString("/")

  def editUri = mainPath + "?mode=edit"

  def historyUri = mainPath + "?mode=history"

  def revisionUri(pid: ObjectId) = mainPath + s"?mode=revision&rid=$pid"

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
      case Full("history") => history
      case Full("revision") => byRevision
      case _ => normal
    }
  }


  def byRevision: NodeSeq = {
    val rid = S.param("rid").flatMap(XOid.unapply).toOption
    val wiki = rid.flatMap { id => WikiPageRecord where (_.id eqs id) get() }
    renderItem(wiki)
  }

  def formatSize(sz: Long) = {
    if (sz > 1024) {
      val dsz = sz / 1024.0
      "%.2f".format(dsz)
    } else sz.toString
  }

  def renderHistory(items: List[WikiHistoryItem]) = {
    import Helpers._

    ".wiki-history-entry" #> {ns: NodeSeq =>
      items.flatMap{ i =>
        val css =
          ".date" #> <a href={revisionUri(i.id)}>{Formatting.format(i.date)}</a> &
          ".username" #> i.user &
          ".comment" #> i.comment &
          ".size" #> formatSize(i.size)
        css.apply(ns)
      }
    } &
    ".backlink" #> <a class="btn" href={mainPath}>Page</a>
  }


  def history: NodeSeq = {
    val skip = S.param("skip").flatMap(Helpers.asInt).toOption
    val pages = WikiPageRecord where (_.path eqs path) orderDesc(_.datetime) select(
        _.id, _.datetime, _.editor, _.comment, _.size
    ) skipOpt(skip) fetch(50)
    val uids = pages map (_._3) toSet
    val users = UserRecord where (_.id in uids) select(_.id, _.username) fetch() toMap
    val nodes = Templates("templates-hidden" :: "wiki-history" :: Nil)
    val items = pages.map {
      case (pid, date, uid, comment, size) =>
        WikiHistoryItem(pid, users.get(uid).getOrElse("unknown"), date, comment, size)
    }
    nodes match {
      case Full(c) => renderHistory(items)(c)
      case _ => NodeSeq.Empty
    }
  }

  def normal: NodeSeq = {
    val rec = WikiPageRecord where (_.path eqs path) orderDesc (_.datetime) get()
    renderItem(rec)
  }

  def renderItem(rec: Option[WikiPageRecord]): NodeSeq = {
    val head = if (loggedIn) editPanel else Nil
    val body = if (rec.isDefined)
      renderMarkdown(rec.get)
    else
      empty
    head ++ body
  }
}

