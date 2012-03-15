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

package org.eiennohito.kotonoha.web.snippet

import net.liftweb.json.JsonAST.JObject
import net.liftweb.mongodb.{Limit, Skip}
import org.eiennohito.kotonoha.util.{Formatting, Strings}
import net.liftweb.util.{Helpers, BindHelpers}
import util.matching.Regex
import xml.{Elem, Text, NodeSeq}
import net.liftweb.common.{Full, Box}
import net.liftweb.http.{RequestVar, SHtml, SortedPaginatorSnippet, S}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.SetHtml
import org.eiennohito.kotonoha.records.{ExampleRecord, UserRecord, WordRecord}

/**
 * @author eiennohito
 * @since 15.03.12
 */

object WordSnippet {

  def wordId: Box[Long] = {
    S.param("w") map (java.lang.Long.parseLong(_, 16))
  }

  object word extends RequestVar[Box[WordRecord]](wordId flatMap {WordRecord.find(_)})

  def save(rec: WordRecord) : JsCmd = {
    rec.save
    SetHtml("status", <b>Saved!</b>)
  }

  def renderForm(in: NodeSeq): NodeSeq = {
    import Helpers._
    import org.eiennohito.kotonoha.util.DateTimeUtils._
     word.is match {
      case Full(w) => {
        bind("word", SHtml.ajaxForm(in),
          "createdon" -> Formatting.format(w.createdOn.is),
          "writing" -> w.writing.toForm,
          "reading" -> w.reading.toForm,
          "meaning" -> w.meaning.toForm,
          "submit" -> SHtml.ajaxSubmit("Save", () => save(w)))
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
        <td class="kanji" width="50%">
          <ex:example></ex:example>
        </td>
        <td width="50%">
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

  def exampleAjaxForm(in: NodeSeq): NodeSeq = {
    SHtml.ajaxForm(in)
  }

}

class WordPaginator extends SortedPaginatorSnippet[WordRecord, String] {
  import org.eiennohito.kotonoha.util.KBsonDSL._

  def headers = ("adate" -> "createdOn" ) :: ("writing" -> "writing") :: ("reading" -> "reading") :: Nil

  lazy val count = WordRecord.count(query)

  override def itemsPerPage = 50

  def searchQuery = {
    S.param("q") openOr ""
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
    import org.eiennohito.kotonoha.util.DateTimeUtils._

    def v(id: Long) =  {
      val link = "detail?w=%s".format(id.toHexString)
      AttrBindParam("link", Text("javascript:Navigate(\"" + link + "\");"), "onclick")
    }
    page.flatMap {i =>
      bind("word", in,
        v(i.id.is),
        "addeddate" -> Formatting.format(i.createdOn.is),
        "reading" -> i.reading.is,
        "writing" -> i.writing.is,
        "meaning" -> Strings.substr(i.meaning.is, 50)
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
