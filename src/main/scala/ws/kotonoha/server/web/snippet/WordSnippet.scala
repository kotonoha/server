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

import net.liftweb.json.JsonAST.{JString, JArray, JObject}
import net.liftweb.mongodb.{Limit, Skip}
import net.liftweb.util.{Helpers, BindHelpers}
import util.matching.Regex
import xml.{Elem, Text, NodeSeq}
import net.liftweb.common.{Full, Box}
import net.liftweb.http.{RequestVar, SHtml, SortedPaginatorSnippet, S}
import net.liftweb.http.js.{JE, JsCmd}
import net.liftweb.http.js.JsCmds.SetHtml
import com.foursquare.rogue.Rogue
import ws.kotonoha.server.records._
import ws.kotonoha.server.util.{DateTimeUtils, ParseUtil, Formatting, Strings}
import ws.kotonoha.server.model.CardMode
import org.joda.time.Period
import ws.kotonoha.server.actors.ioc.{Akka, ReleaseAkka}
import ws.kotonoha.server.actors.model.{MarkForDeletion, ChangeWordStatus}
import ws.kotonoha.server.util.unapply.XHexLong

/**
 * @author eiennohito
 * @since 15.03.12
 */

object WordSnippet extends Akka with ReleaseAkka {

  def wordId: Box[Long] = {
    S.param("w") map (ParseUtil.hexLong(_))
  }

  object word extends RequestVar[Box[WordRecord]](wordId flatMap {WordRecord.find(_)})

  def save(rec: WordRecord) : JsCmd = {
    rec.save
    SetHtml("status", <b>Saved!</b>)
  }

  def sna(rec: WordRecord): JsCmd = {
    akkaServ ! ChangeWordStatus(rec.id.is, WordStatus.Approved)
    SetHtml("word-status", Text("Approved")) & SetHtml("status", <b>Saved</b>)
  }

  def renderForm(in: NodeSeq): NodeSeq = {
    import Helpers._
    import ws.kotonoha.server.util.DateTimeUtils._
     word.is match {
      case Full(w) => {
        bind("word", SHtml.ajaxForm(in),
          "createdon" -> Formatting.format(w.createdOn.is),
          "writing" -> w.writing.toForm,
          "reading" -> w.reading.toForm,
          "meaning" -> w.meaning.toForm,
          "status" -> w.status.is.toString,
          "submit" -> SHtml.ajaxSubmit("Save", () => save(w)),
          "sna" -> SHtml.ajaxSubmit("Save & Approve", () => sna(w)))
      }
      case _ => S.error("Invalid word"); <em>Invalid word</em>
    }
  }

  def addExample(record: WordRecord) : JsCmd = {
    val exs = record.examples.is
    val w = record.examples(exs ++ List(ExampleRecord.createRecord))
    word(Full(w))
    SetHtml("extable", renderExamples)
  }

  def renderExamples: NodeSeq = {
    import Helpers._
    val templ = <tr xmlns:ex="example">
        <td class="nihongo full" width="50%">
          <ex:example></ex:example>
        </td>
        <td class="full" width="50%">
          <ex:translation></ex:translation>
        </td>
      </tr>
    val inner = word.is match {
      case Full(w) => {
        w.examples.is.flatMap {
          ex =>
            bind("ex", templ,
              "example" -> ex.example.toForm,
              "translation" -> ex.translation.toForm)
        } ++
        <tr>
          <td></td>
          <td>{SHtml.ajaxSubmit("Add new example", () => addExample(w))}</td>
        </tr>
        <tr>
          <td></td>
          <td>{SHtml.ajaxSubmit("Save", () => save(w))}</td>
        </tr>
      }
      case _ => <b>No word, no examples</b>
    }
    inner
  }

  def renderExamples(in: NodeSeq): NodeSeq = {
    val inner = renderExamples
    in.head.flatMap {
      case e: Elem => Elem(e.prefix, e.label, e.attributes, e.scope, e.child ++ inner : _*)
      case _ => in
    }
  }

  def renderLearning(box: Box[ItemLearningDataRecord]): NodeSeq = box match {
    case Full(il) => {
      import ws.kotonoha.server.math.MathUtil.round
      import DateTimeUtils._

      val period = {
        def render(p: Period) = {
          import org.joda.time.DurationFieldType._
          val list = List("Year" -> years(), "Month" -> months(), "Week" -> weeks(), "Day" -> days(), "Hour" -> hours(), "Minute" -> minutes(), "Second" -> seconds())
          val sb = new StringBuilder
          list.foreach {
            case (s, tp) => {
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

        val prd = new Period(now, il.intervalEnd.is)
        val negative = prd.getValues.count(_ < 0) > 0
        if (negative) {
          render(prd.negated()) + "ago"
        } else {
          "in " + render(prd)
        }
      }

      <div>Difficulty: {round(il.difficulty.is, 2)}</div> ++
      <div>Scheduled on: {Formatting.format(il.intervalEnd.is)}, {period}</div> ++
      <div>Has {il.repetition.is} repetition and {il.lapse.is} lapse</div>
    }
    case _ => Text("")
  }

  def mode(m: Int) = m match {
    case CardMode.READING => "Reading"
    case CardMode.WRITING => "Writing"
    case _ => "Unknown"
  }

  def renderCards(in: NodeSeq): NodeSeq = {
    import Helpers._
    import Rogue._
    val cards = WordCardRecord where (_.word eqs wordId.get) orderAsc (_.cardMode) fetch()
    cards.flatMap { c =>
      bind("wc", in,
        "mode" -> <h5>{mode(c.cardMode.is).+(" card")}</h5>,
        "learning" -> renderLearning(c.learning.valueBox)
      )
    }
  }

  def exampleAjaxForm(in: NodeSeq): NodeSeq = {
    SHtml.ajaxForm(in)
  }

}

class WordPaginator extends SortedPaginatorSnippet[WordRecord, String] with Akka with ReleaseAkka {
  import ws.kotonoha.server.util.KBsonDSL._

  def headers = ("adate" -> "createdOn" ) :: ("status" -> "status") :: ("writing" -> "writing") :: ("reading" -> "reading") :: Nil

  lazy val count = WordRecord.count(query)

  override def itemsPerPage = 50

  def searchQuery = {
    S.param("q") openOr ""
  }

  def findIds(s: String) = {
    s.split('&') flatMap {
      _.split('=') match {
        case Array(XHexLong(id), _) => Some(id)
        case _ => None
      }
    }
  }

  def ajaxReq(in: NodeSeq) = {
    import net.liftweb.json.{compact, render}
    def handler(s: String): JsCmd = {
      val ids = findIds(s)
      ids foreach { akkaServ ! ChangeWordStatus(_, WordStatus.ReviewWord) }
      val data = JArray(ids.toList.map{ l => JString(l.toHexString) } )
      val x = compact(render(data))
      JE.Call("update_data", JE.JsRaw(x), JE.Str("ReviewWord") ).cmd
    }
    def handler_delete(s: String): JsCmd = {
      val ids = findIds(s)
      ids foreach { akkaServ ! MarkForDeletion(_) }
      val data = JArray(ids.toList.map{ l => JString(l.toHexString) } )
      val x = compact(render(data))
      JE.Call("update_data", JE.JsRaw(x), JE.Str("Deleting") ).cmd
    }
    def handler_approveall(s: String): JsCmd = {
      val ids = findIds(s)
      ids foreach { akkaServ ! ChangeWordStatus(_, WordStatus.Approved) }
      val data = JArray(ids.toList map { i => JString(i.toHexString) } )
      val x = compact(render(data))
      JE.Call("update_data", JE.JsRaw(x), JE.Str("Approved")).cmd
    }
    SHtml.ajaxButton(Text("Mark for review"), JE.Call("list_data") , handler _) ++
    SHtml.ajaxButton(Text("Delete"), JE.Call("list_data") , handler_delete _) ++
    SHtml.ajaxButton(Text("Approve"), JE.Call("list_data"), handler_approveall _)
  }


  override def sortedPageUrl(offset: Long, sort: (Int, Boolean)) = {
    import net.liftweb.util.Helpers
    Helpers.appendParams(super.sortedPageUrl(offset, sort), List("q" -> searchQuery))
  }

  def query : JObject = {
    val init = ("user" -> UserRecord.currentId.openTheBox)
    searchQuery match {
      case "" => init
      case q => {
        val rq = new Regex(q)
        init ~ ("$or" -> List(("reading" -> rq), ("writing" -> rq)))
      }
    }
  }


  def sortObj : JObject = {
    val (col, direction) = sort
    val sortint = if (direction) 1 else -1
    (headers(col)._2 -> sortint)
  }

  def queryVal(in: NodeSeq) = {
    import Helpers._
    in.map {
      case e: Elem => e % ("value" -> searchQuery)
      case x @ _ => x
    }
  }

  def page = {
    WordRecord.findAll(query, sortObj, Skip(curPage * itemsPerPage.toInt), Limit(itemsPerPage))
  }

  def renderPage(in: NodeSeq): NodeSeq = {
    import BindHelpers._
    import ws.kotonoha.server.util.DateTimeUtils._

    def v(id: Long) =  {
      val link = "row-%s".format(id.toHexString)
      AttrBindParam("row", Text(link), "id")
    }

    page.flatMap {i =>
      bind("word", in,
        v(i.id.is),
        "selected" -> <input type="checkbox" name={i.id.is.toHexString} /> ,
        "addeddate" -> Formatting.format(i.createdOn.is),
        "reading" -> i.reading.is,
        "writing" -> <a href={"detail?w="+ i.id.is.toHexString}>{i.writing.is}</a>,
        "meaning" -> Strings.substr(i.meaning.is, 50),
        "status" -> i.status.is.toString
      )
    }
  }

  def func(in: String) = {}

  def params(in: NodeSeq): NodeSeq = {
    val (ap, sp) = sort
    List(SHtml.hidden(func _, curPage * itemsPerPage toString, "name" -> offsetParam),
      SHtml.hidden(func _, sp.toString, "name" -> ascendingParam),
      SHtml.hidden(func _, ap.toString, "name" -> sortParam))
  }
}
