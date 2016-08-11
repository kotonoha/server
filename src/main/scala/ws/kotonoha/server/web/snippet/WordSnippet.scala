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

import net.liftweb.common.{Box, Full}
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.{JE, JsCmd}
import net.liftweb.http.{RequestVar, S, SHtml, SortedPaginatorSnippet}
import net.liftweb.json.JsonAST.{JArray, JObject, JString}
import net.liftweb.mongodb.{Limit, Skip}
import net.liftweb.util.ControlHelpers.tryo
import org.bson.types.ObjectId
import org.joda.time.Period
import ws.kotonoha.model.{CardMode, WordStatus}
import ws.kotonoha.server.actors.ioc.{Akka, ReleaseAkka}
import ws.kotonoha.server.actors.model.{ChangeWordStatus, MarkForDeletion}
import ws.kotonoha.server.records._
import ws.kotonoha.server.util.unapply.XOid
import ws.kotonoha.server.util.{DateTimeUtils, Formatting, Strings}

import scala.util.matching.Regex
import scala.xml.{Elem, NodeSeq, Text}

/**
  * @author eiennohito
  * @since 15.03.12
  */

object WordSnippet {

  def wordId: Box[ObjectId] = {
    S.param("w") flatMap (x => tryo {
      new ObjectId(x)
    })
  }


  def renderLearning(box: Box[ItemLearningDataRecord]): NodeSeq = box match {
    case Full(il) => {
      import DateTimeUtils._
      import ws.kotonoha.server.math.MathUtil.round

      val period = {
        def render(p: Period) = {
          import org.joda.time.DurationFieldType._
          val list = List(
            "Year" -> years(),
            "Month" -> months(),
            "Week" -> weeks(),
            "Day" -> days(),
            "Hour" -> hours(),
            "Minute" -> minutes(),
            "Second" -> seconds()
          )
          val sb = new StringBuilder
          list.foreach {
            case (s: String, tp) => {
              val i = p.get(tp)
              if (i != 0) {
                sb.append(i)
                sb.append(" ")
                sb.append(s)
                if (i % 10 != 1 || i == 11) {
                  sb.append("s")
                }
                sb.append(" ")
              }
            }
          }
          sb.toString()
        }

        val prd = new Period(now, il.intervalEnd.get)
        val negative = prd.getValues.count(_ < 0) > 0
        if (negative) {
          render(prd.negated()) + "ago"
        } else {
          "in " + render(prd)
        }
      }

      <div>Difficulty:
        {round(il.difficulty.get, 2)}
      </div> ++
        <div>Scheduled on:
          {Formatting.format(il.intervalEnd.get)}
          ,
          {period}
        </div> ++
        <div>Has
          {il.repetition.get}
          repetition and
          {il.lapse.get}
          lapse</div>
    }
    case _ => Text("Not yet scheduled")
  }

  def mode(m: CardMode) = m match {
    case CardMode.Reading => "Reading"
    case CardMode.Writing => "Writing"
    case _ => "Unknown"
  }

  def renderCards(in: NodeSeq): NodeSeq = {
    import ws.kotonoha.server.web.lift.Binders._
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    val cards = WordCardRecord where (_.word eqs wordId.openOrThrowException("should be present")) orderAsc (_.cardMode) fetch()


    val tf = cards.map { c =>
      ".card-mode *" #> mode(c.cardMode.get).+(" card") &
      ".card-learning" #> renderLearning(c.learning.valueBox)
    }

    ("^" #> tf).apply(in)
  }

  def exampleAjaxForm(in: NodeSeq): NodeSeq = {
    SHtml.ajaxForm(in)
  }

}

class WordPaginator extends SortedPaginatorSnippet[WordRecord, String] with Akka with ReleaseAkka {

  import ws.kotonoha.server.actors.UserSupport._
  import ws.kotonoha.server.util.KBsonDSL._

  def headers = ("adate" -> "createdOn") :: ("status" -> "status") :: ("writing" -> "writing") :: ("reading" -> "reading") :: Nil

  lazy val count = WordRecord.count(query)

  lazy val uid = UserRecord.currentId.openOrThrowException("No user logged in")

  override def itemsPerPage = 50

  private val searchQuery = S.param("q") openOr ""

  def findIds(s: String): Seq[ObjectId] = {
    s.split('&') flatMap {
      _.split('=') match {
        case Array(XOid(id), _) => Some(id)
        case _ => None
      }
    }
  }

  def ajaxReq(in: NodeSeq) = {
    import net.liftweb.json.{compact, render}
    def handler(s: String): JsCmd = {
      val ids = findIds(s)
      ids.foreach {
        akkaServ ! ChangeWordStatus(_, WordStatus.ReviewWord).u(uid)
      }
      val data = JArray(ids.toList.map { l => JString(l.toString) })
      val x = compact(render(data))
      JE.Call("update_data", JE.JsRaw(x), JE.Str("ReviewWord")).cmd
    }

    def handler_delete(s: String): JsCmd = {
      val ids = findIds(s)
      ids.foreach {
        akkaServ ! MarkForDeletion(_).u(uid)
      }
      val data = JArray(ids.toList.map { l => JString(l.toString) })
      val x = compact(render(data))
      JE.Call("update_data", JE.JsRaw(x), JE.Str("Deleting")).cmd
    }

    def handler_approveall(s: String): JsCmd = {
      val ids = findIds(s)
      ids foreach {
        akkaServ ! ChangeWordStatus(_, WordStatus.Approved).u(uid)
      }
      val data = JArray(ids.toList map { i => JString(i.toString) })
      val x = compact(render(data))
      JE.Call("update_data", JE.JsRaw(x), JE.Str("Approved")).cmd
    }

    SHtml.ajaxButton(Text("Mark for review"), JE.Call("list_data"), handler _) ++
      SHtml.ajaxButton(Text("Delete"), JE.Call("list_data"), handler_delete _) ++
      SHtml.ajaxButton(Text("Approve"), JE.Call("list_data"), handler_approveall _)
  }


  override def sortedPageUrl(offset: Long, sort: (Int, Boolean)) = {
    import net.liftweb.util.Helpers
    Helpers.appendParams(super.sortedPageUrl(offset, sort), List("q" -> searchQuery))
  }

  def query: JObject = {
    val init = "user" -> uid
    searchQuery match {
      case "" => init
      case q => {
        val rq = new Regex(q)
        init ~ ("$or" -> List("reading" -> rq, "writing" -> rq))
      }
    }
  }


  def sortObj: JObject = {
    val (col, direction) = sort
    val sortint = if (direction) 1 else -1
    (headers(col)._2 -> sortint)
  }

  import ws.kotonoha.server.web.lift.Binders._


  def page = {
    val toskip = curPage * itemsPerPage
    WordRecord.findAll(query, sortObj, Skip(if (toskip >= count) 0 else toskip), Limit(itemsPerPage))
  }

  def renderPage(in: NodeSeq): NodeSeq = {

    def linkDetail(i: WordRecord) = {
      <a href={"detail?w=" + i.id.get.toString}>
        {i.writing.stris}
      </a>
    }

    val fn = bseq(page) { i =>
      "tr [id]" #> i.id &
      ";selected *" #> <input type="checkbox" name={i.id.get.toString}></input> &
      ";addedDate *" #> Formatting.format(i.createdOn.get) &
      ";reading *" #> i.reading.stris &
      ";writing *" #> linkDetail(i) &
      ";meaning *" #> Strings.substr(i.meaning.get, 50) &
      ";status *" #> i.status.get.toString
    }

    fn(in)
  }

  def func(in: String) = {}

  def params(in: NodeSeq): NodeSeq = {
    val (ap, sp) = sort
    val hidden: NodeSeq = List(SHtml.hidden(func _, curPage * itemsPerPage toString, "name" -> offsetParam),
      SHtml.hidden(func _, sp.toString, "name" -> ascendingParam),
      SHtml.hidden(func _, ap.toString, "name" -> sortParam))

    logger.info(searchQuery)

    val tf =
      "@q [value]" #> searchQuery &
      ";other" #> hidden

    tf(in)
  }
}
