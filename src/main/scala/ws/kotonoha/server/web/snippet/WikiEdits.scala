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

import net.liftweb.http.{RequestVar, PaginatorSnippet, DispatchSnippet}
import ws.kotonoha.server.records.{UserRecord, WikiPageRecord}
import org.joda.time.DateTime
import org.bson.types.ObjectId
import scala.xml.NodeSeq
import ws.kotonoha.server.util.Formatting

/**
 * @author eiennohito
 * @since 29.04.13 
 */

object WikiEdits extends DispatchSnippet {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  object paginator extends RequestVar[EditPaginator](new EditPaginator)

  def wikilink(page: String) = {
    val href = "/wiki/" + page
    <a href={href}>{page}</a>
  }

  def render(ns: NodeSeq): NodeSeq = {
    import net.liftweb.util.Helpers._
    val pg = paginator.is
    val items = pg.page
    val uids = items.map(_.user).toSet
    val users = UserRecord where (_.id in uids) select(_.id, _.username) fetch() toMap ;
    items.flatMap { li =>
      val css =
        ".date" #> Formatting.format(li.date) &
        ".page" #> wikilink(li.path) &
        ".user" #> users.get(li.user).getOrElse("unknown") &
        ".comment" #> li.comment &
        ".size" #> li.size
      css(ns)
    }
  }

  def dispatch = {
    case "render" => render
    case "pagination" => paginator.is.paginate
  }
}

case class WikiEdit(path: String, date: DateTime, user: ObjectId, comment: String, size: Long)

class EditPaginator extends PaginatorSnippet[WikiEdit] {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._


  override def itemsPerPage = 50

  def count = WikiPageRecord count()

  def page = WikiPageRecord orderDesc(_.datetime) selectCase (
      _.path, _.datetime, _.editor, _.comment, _.size, WikiEdit
    ) skip(first.toInt) fetch(itemsPerPage)
}
