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

import xml.{Text, NodeSeq}
import net.liftweb.http.{DispatchSnippet, S}
import net.liftweb.common.Full
import net.liftweb.util.Props

/**
 * @author eiennohito
 * @since 31.08.12
 */

/**
 * Classpath resource
 */
object ClasspathResource extends DispatchSnippet {

  def min(in: String) = {
    if (Props.devMode) {
      in
    } else {
      in + ".min"
    }
  }

  def js(in: String) = in + ".js"

  def css(in: String): String = in + ".css"

  def minifiedJs(nfo: String): String = {
    js(min(nfo))
  }

  val basepath = "/classpath/cpres/"

  def script(in: NodeSeq) = {
    S.attr("src") match {
      case Full(x) => <script type="text/javascript" src={basepath+js(min(x))}></script>
      case _ => Nil
    }
  }

  def css(in: NodeSeq): NodeSeq = {
    S.attr("src") match {
      case Full(x) =>
        <link href={basepath+css(min(x))} type="text/css" rel="stylesheet"></link>
      case _ => Nil
    }
  }

  def dispatch = {
    case "script" => script
    case "css" => css
    case _ => script
  }
}


