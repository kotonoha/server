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
import net.liftweb.http.S
import ws.kotonoha.server.actors.dict.{ExampleEntry, _}
import ws.kotonoha.server.actors.{GlobalActors, SearchQuery}
import ws.kotonoha.server.util.LangUtil

import scala.concurrent.{Await, ExecutionContext}
import scala.xml.NodeSeq


/**
 * @author eiennohito
 * @since 19.04.12
 */

class ExampleDisplay @Inject() (
  akka: GlobalActors
)(implicit ec: ExecutionContext) {
  import ws.kotonoha.server.web.lift.Binders._

  import scala.concurrent.duration._

  private val query = S.param("query").getOrElse("食べる")


  def fld(in: NodeSeq): NodeSeq = {
    val fn =
      "@query [value]" #> query

    fn(in)
  }

  //small wtf, build list of examples and translations from query, asynchronously ftw
  def docs: List[ExampleEntry] = {
    val f = akka.gask[List[Long]](SearchQuery(query))
    val fs = f flatMap  { cand =>
        akka.gask[List[ExampleEntry]](FullLoadExamples(cand, LangUtil.langs))
    }
    Await.result(fs, 5.seconds)
  }

  def body(in: NodeSeq): NodeSeq = {
    val tf = (e: ExampleEntry) =>
      ".original *" #> e.jap.content &
      ".translation" #> e.other.map { o => ".translation *+" #> o.content }

    bseq(in, docs, tf)
  }
}
