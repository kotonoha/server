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

import com.google.inject.Inject
import net.liftweb.common.{Box, Full}
import net.liftweb.http.js.{JE, JsCmd, JsExp}
import net.liftweb.http.{S, SHtml, SortedPaginatorSnippet}
import net.liftweb.json.JsonAST.{JArray, JObject, JString}
import net.liftweb.mongodb.{Limit, Skip}
import org.bson.types.ObjectId
import org.joda.time.{DateTime, Period}
import ws.kotonoha.model.{CardMode, RepExampleStatus, WordStatus}
import ws.kotonoha.server.actors.AkkaMain
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.ops.{WordExampleOps, WordJmdictOps, WordOps}
import ws.kotonoha.server.records._
import ws.kotonoha.server.util._
import ws.kotonoha.server.util.unapply.XOid

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex
import scala.xml._

/**
  * @author eiennohito
  * @since 15.03.12
  */

class WordSnippet @Inject() (
  wj: WordJmdictOps,
  weo: WordExampleOps
)(implicit ec: ExecutionContext) {
  import ws.kotonoha.server.web.lift.Binders._

  val wordId: Box[ObjectId] = S.param("w").flatMap(XOid.unapply)

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

class WordPaginator @Inject() (
  wo: WordOps,
  uc: UserContext,
  akkaServ: AkkaMain
) extends SortedPaginatorSnippet[WordRecord, String] {

  import ws.kotonoha.server.util.KBsonDSL._

  def headers = ("adate" -> "createdOn") :: ("status" -> "status") :: ("writing" -> "writing") :: ("reading" -> "reading") :: Nil

  lazy val count = WordRecord.count(query)

  private def uid = uc.uid

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

  def ajaxReq(in: NodeSeq): NodeSeq = {

    def idsString(ids: Seq[ObjectId]): JsExp = {
      val data = JArray(ids.map { l => JString(l.toString) }.toList)
      data
    }


    def handlerStopRepeat(s: String): JsCmd = {
      val ids = findIds(s)
      wo.changeStatus(ids, WordStatus.ReviewWord)
      JE.Call("update_data", idsString(ids), JE.Str(statusReview.toString())).cmd
    }

    def handler_delete(s: String): JsCmd = {
      val ids = findIds(s)
      val delDate = DateTimeUtils.now.plusDays(7)
      wo.markForDeletion(ids, delDate) //delete stuff after 7 days
      JE.Call("update_data", idsString(ids), JE.Str(statusDeleting(delDate).toString())).cmd
    }

    def handler_approveall(s: String): JsCmd = {
      val ids = findIds(s)
      wo.changeStatus(ids, WordStatus.Approved)
      JE.Call("update_data", idsString(ids), JE.Str(statusApproved.toString())).cmd
    }

    <span>
      {SHtml.ajaxButton(Text("Stop repeat"), JE.Call("list_data"), handlerStopRepeat _, "class" -> "btn btn-outline-primary btn-sm")}
      {SHtml.ajaxButton(Text("Delete"), JE.Call("list_data"), handler_delete _, "class" -> "btn btn-outline-primary btn-sm")}
      {SHtml.ajaxButton(Text("Approve"), JE.Call("list_data"), handler_approveall _, "class" -> "btn btn-outline-primary btn-sm")}
    </span>
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
    headers(col)._2 -> sortint
  }

  import ws.kotonoha.server.web.lift.Binders._


  def page = {
    val toskip = curPage * itemsPerPage
    WordRecord.findAll(query, sortObj, Skip(if (toskip >= count) 0 else toskip), Limit(itemsPerPage))
  }

  private def icon(cls: String, classes: String = ""): Elem = {
    val allClasses = s"fa fa-$cls $classes"

    <i class={allClasses}></i>
  }

  def tooltip(name: String): MetaData = new UnprefixedAttribute("title", name, Null)


  private val statusNew = icon("leaf") % tooltip("New word, will not be repeated")
  private val statusApproved = icon("check-circle-o") % tooltip("Approved word, will be repeated")
  private val statusReview = icon("question-circle-o") % tooltip("Word will not be repeated")
  private def statusDeleting(on: DateTime) = icon("trash") % tooltip(s"Word will be deleted on ${Formatting.format(on)}")

  private def statusOf(rec: WordRecord): NodeSeq = {
    val ws = rec.status.get
    val s1 = ws match {
      case WordStatus.New => statusNew
      case WordStatus.Approved => statusApproved
      case WordStatus.ReviewWord | WordStatus.ReviewExamples => statusReview
      case WordStatus.Deleting => statusDeleting(rec.deleteOn.get)
      case _ => Text(ws.toString())
    }

    val s2 = rec.repExStatus.get match {
      case RepExampleStatus.Fresh => icon("circle-thin") % tooltip("Examples for the word are not acquired yet")
      case RepExampleStatus.Present => icon("circle", "autoex-ok") % tooltip("Repetition examples are present")
      case RepExampleStatus.EmptyResponse => icon("circle", "autoex-empty") % tooltip("Examples for word are not supported yet")
      case RepExampleStatus.Failure => icon("circle", "autoex-fail") % tooltip("Examples acquire failed")
      case _ => Text(ws.toString())
    }

    <span>
      <span class="word-status">{s1}</span>
      <span class="word-ex-status">{s2}</span>
    </span>
  }

  def renderPage(in: NodeSeq): NodeSeq = {

    def linkDetail(i: WordRecord) = {
      <a href={"detail?w=" + i.id.get.toString}>
        {i.writing.stris}
      </a>
    }

    val fn = bseq(page) { i =>
      "tr [id]" #> s"row-${i.id}" &
      ";selected *" #> <input type="checkbox" name={i.id.get.toString}></input> &
      ";addedDate *" #> Formatting.format(i.createdOn.get) &
      ";reading *" #> i.reading.stris &
      ";writing *" #> linkDetail(i) &
      ";meaning *" #> Strings.substr(i.meaning.get, 50) &
      ";status *" #> statusOf(i)
    }

    fn(in)
  }

  def func(in: String) = {}

  def params(in: NodeSeq): NodeSeq = {
    val (ap, sp) = sort
    val hidden: NodeSeq = List(SHtml.hidden(func _, curPage * itemsPerPage toString, "name" -> offsetParam),
      SHtml.hidden(func _, sp.toString, "name" -> ascendingParam),
      SHtml.hidden(func _, ap.toString, "name" -> sortParam))

    val tf =
      "@q [value]" #> searchQuery &
      ";other" #> hidden

    tf(in)
  }
}
