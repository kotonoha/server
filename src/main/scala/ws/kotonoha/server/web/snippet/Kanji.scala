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

import net.liftweb.http.{S, DispatchSnippet}
import scala.xml.{Text, NodeSeq}
import net.liftweb.common.Full
import net.liftweb.util.Helpers
import ws.kotonoha.akane.kanji.kradfile.{RadicalDb, SimilarKanji}

/**
 * @author eiennohito
 * @since 08.07.13 
 */

object Kanji extends DispatchSnippet {
  def dispatch = {
    case "list" => render
    case "form" => form
  }

  def form(in: NodeSeq): NodeSeq = {
    import Helpers._
    bind("k", in,
      AttrBindParam("value", S.param("kanji").getOrElse(""), "value")
    )
  }

  def render(in: NodeSeq): NodeSeq = {
    val req = S.param("kanji")
    req match {
      case Full(s) => {
        import Helpers._
        val data = SimilarKanji.find(s)
        val decomp = RadicalDb.table.get(s).toSeq.flatten
        val ds = Text(s"Kanji decomposition is: ${decomp.mkString(",")}")
        val out = data.take(40).flatMap(k =>
        bind("k", in,
          "text" -> Text(k.text),
          "common" -> Text(k.common.mkString(",")),
          "diff" -> Text(k.diff.mkString(","))
        ))
        ds ++ out
      }
      case _ => <div>Invalid kanji, nothing found</div>
    }
  }
}
