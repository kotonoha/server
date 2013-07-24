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
import ws.kotonoha.server.records.events.MarkEventRecord
import org.bson.types.ObjectId
import ws.kotonoha.server.records.{WordRecord, WordCardRecord, UserRecord}
import scala.xml.{Text, NodeSeq}
import ws.kotonoha.server.util.Formatting
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.server.model.CardMode

/**
 * @author eiennohito
 * @since 29.04.13 
 */

object LearningHistory extends DispatchSnippet {
  object paginator extends RequestVar[LearningHistoryPaginator](
    new LearningHistoryPaginator(UserRecord.currentId.openOr(new ObjectId))
  )


  def renderMarks(ns: NodeSeq): NodeSeq = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    import net.liftweb.util.Helpers._

    val marks = paginator.is.page
    val cids = marks.map(_.card.is)
    val cards = WordCardRecord where (_.id in cids) select(_.id, _.word) fetch() toMap
    val words = WordRecord where (_.id in (cards.map(_._2))) fetch() map(x => x.id.is -> x) toMap
    def search(in: ObjectId) = cards.get(in).flatMap(x => words.get(x))

    marks.flatMap { m =>
      val word = search(m.card.is)
      val wlink = word match {
        case None => NodeSeq.Empty
        case Some(w) =>
          val href = s"/words/detail?w=${w.id.is}"
          val cont = (w.writing.is.headOption, w.reading.is.headOption) match {
            case (Some(wr), Some(rd)) =>
              if (UnicodeUtil.hasKanji(wr)) {
                <ruby><rb>{wr}</rb><rt>{rd}</rt></ruby>
              } else Text(wr)
            case (Some(wr), _) => Text(wr)
            case (_, Some(rd)) => Text(rd)
            case _ => NodeSeq.Empty
          }
          <a href={href}>{cont}</a>
      }
      val mode = m.mode.is match {
        case CardMode.WRITING => "Writing"
        case CardMode.READING => "Reading"
        case _ => "Unknown"
      }
      val client = m.client.is match {
        case "web-repeat" => "Web"
        case "kotonoha-main" => "Android"
        case x => x
      }
      val css =
        ".date" #> Formatting.format(m.datetime.is) &
        ".word" #> wlink &
        ".kind" #> mode &
        ".mark" #> m.mark.is.toInt &
        ".showtime" #> m.time.is &
        ".client" #> client
      css(ns)
    }
  }

  def dispatch = {
    case "paginate" => paginator.is.paginate
    case "marks" => renderMarks
  }
}

class LearningHistoryPaginator(uid: ObjectId) extends PaginatorSnippet[MarkEventRecord] {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  override def itemsPerPage = 200

  lazy val count = {
    MarkEventRecord where (_.user eqs uid) count()
  }

  lazy val page = MarkEventRecord where (_.user eqs uid) orderDesc(_.datetime) skip(first.toInt) fetch(itemsPerPage)
}
