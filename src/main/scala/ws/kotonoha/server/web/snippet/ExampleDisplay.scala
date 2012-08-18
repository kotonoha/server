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

import xml.{NodeSeq, MetaData}
import ws.kotonoha.server.actors.ioc.{ReleaseAkka, Akka}
import ws.kotonoha.server.records.dictionary.ExampleSentenceRecord
import ws.kotonoha.server.actors.SearchQuery
import ws.kotonoha.server.dict.TatoebaLinks
import java.io.File
import net.liftweb.util.{Props, Helpers}
import net.liftweb.http.{SHtml, S, RequestVar}
import ws.kotonoha.server.util.LangUtil
import akka.dispatch.{Future, Await}
import ws.kotonoha.server.actors.dict.{ExampleEntry, LoadExamples, ExampleIds, TranslationsWithLangs}


/**
 * @author eiennohito
 * @since 19.04.12
 */

trait ExampleDisplay extends Akka {
  object query extends RequestVar[String](S.param("query").openOr(""))
  import Helpers._
  import ws.kotonoha.server.util.DateTimeUtils._
  import com.foursquare.rogue.Rogue._



  def fld(in: NodeSeq): NodeSeq = {
    bind("ef", in, "fld" -> SHtml.text(query.is, query(_), "name" -> "query"))
  }

  //small wtf, build list of examples and translations from query, asynchronously ftw
  def docs: List[ExampleEntry] = {
    val f = (akkaServ ? SearchQuery(query.is)).mapTo[List[Long]]
    val fs = f flatMap { cand => {
      val comp = (akkaServ ? TranslationsWithLangs(cand, LangUtil.langs)).mapTo[List[ExampleIds]]
      val recs = comp.flatMap(akkaServ ? LoadExamples(_)).mapTo[List[ExampleEntry]]
      recs
    }}
    Await.result(fs, 5 seconds)
  }

  def body(in: NodeSeq): NodeSeq = {
    docs.flatMap( d =>
      <div>{ inner(d) }</div>
    )
  }


  def inner(d: ExampleEntry): NodeSeq = {
    <li>{ d.jap.content.is }</li> ++
    d.other.flatMap(o => <li>{o.content}</li>)
  }
}

object ExampleDisplay extends ExampleDisplay with ReleaseAkka
