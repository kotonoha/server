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

import org.eiennohito.kotonoha.records.{UserRecord, WordRecord}
import net.liftweb.json.JsonAST.JObject
import net.liftweb.mongodb.{Limit, Skip}
import org.eiennohito.kotonoha.util.{Fomatting, Strings}
import net.liftweb.http.{SHtml, SortedPaginatorSnippet, S}
import net.liftweb.util.{Helpers, BindHelpers}
import util.matching.Regex
import xml.{Elem, Text, NodeSeq}

/**
 * @author eiennohito
 * @since 15.03.12
 */

object WordSnippet {
  def listWords(c: NodeSeq):NodeSeq = {
    val words = WordRecord.myWords.fetch(50)
    //BindHelpers.bind("user")
    c
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
        "addeddate" -> Fomatting.format(i.createdOn.is),
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
