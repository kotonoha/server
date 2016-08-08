/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import net.liftweb.http.{DispatchSnippet, S}
import ws.kotonoha.akane.kanji.kradfile.{RadicalDb, SimilarKanji}

import scala.xml.NodeSeq

/**
  * @author eiennohito
  * @since 08.07.13
  */

class Kanji extends DispatchSnippet {

  import ws.kotonoha.server.web.lift.Binders._

  def dispatch = {
    case "list" => render
    case "form" => form
  }

  val query = S.param("kanji").openOr("字")

  val form: NodeSeqFn = {
    "@kanji [value]" #> query
  }

  def render(in: NodeSeq): NodeSeq = {

    val data = SimilarKanji.find(query)
    val decomp = RadicalDb.table.get(query).toSeq.flatten

    val fn =
      ";decomposition" #> decomp.mkString(", ") &
      ".similar" #> bseq(data.take(40)) { k =>
        ";kanji *" #> k.text &
        ";common *" #> k.common.mkString(", ") &
        ";diff *" #> k.diff.mkString(", ")
      }

    fn(in)
  }
}
