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

import xml.{NodeSeq, MetaData}
import org.eiennohito.kotonoha.actors.ioc.{ReleaseAkka, Akka}
import org.eiennohito.kotonoha.records.dictionary.ExampleSentenceRecord
import org.eiennohito.kotonoha.actors.SearchQuery
import akka.dispatch.Await
import org.eiennohito.kotonoha.dict.TatoebaLinks
import java.io.File
import net.liftweb.util.{Props, Helpers}
import net.liftweb.http.{SHtml, S, RequestVar}


/**
 * @author eiennohito
 * @since 19.04.12
 */

trait ExampleDisplay extends Akka {
  object query extends RequestVar[String](S.param("query").openOr(""))
  import Helpers._
  import org.eiennohito.kotonoha.util.DateTimeUtils._
  import com.foursquare.rogue.Rogue._

  val exampleSearcher = new TatoebaLinks(new File(Props.get("example.index").get))

  def fld(in: NodeSeq): NodeSeq = {
    bind("ef", in, "fld" -> SHtml.text(query.is, query(_), "name" -> "query"))
  }

  def okayLang(in: String) = in match {
    case "eng" => true
    case "rus" => true
    case _ => false
  }

  //small wtf, build list of examples and translations from query
  def docs: List[List[ExampleSentenceRecord]] = {
    val f = (akkaServ ? SearchQuery(query.is)).mapTo[List[Long]]
    val candidates = Await.result(f, 1 second)
    val complicated = candidates.map(exampleSearcher.from(_).filter(e => okayLang(e.rightLang)).toList)
    val ids = complicated.flatMap(_.map(l => l.right)) ++ candidates
    val recs = ExampleSentenceRecord where (_.id in ids) fetch()
    val map = recs.map(r => r.id.is -> r).toMap
    val struct = complicated.zip(candidates).map {case (c, cand) =>
      List(map(cand)) ++ c.map(l => map(l.right))
    }
    struct
  }

  def body(in: NodeSeq): NodeSeq = {
    docs.flatMap( d =>
      <div>{ inner(d) }</div>
    )
  }


  def inner(d: scala.List[ExampleSentenceRecord]): NodeSeq = {
    <li>{ d.head.content.is }</li> ++
    d.tail.flatMap(o => <li>{o.content}</li>)
  }
}

object ExampleDisplay extends ExampleDisplay with ReleaseAkka
