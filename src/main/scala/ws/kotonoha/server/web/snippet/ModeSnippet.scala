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

import net.liftweb.http.{S, DispatchSnippet}
import scala.xml.NodeSeq
import net.liftweb.util.Props
import net.liftweb.common.Full

/**
 * @author eiennohito
 * @since 18.04.13 
 */
object ModeSnippet extends DispatchSnippet {

  def is(in: NodeSeq): NodeSeq = {
    val mode = S.attr("name")
    val curmode = Props.modeName
    mode match {
      case Full(`curmode`) => in
      case Full("development") if curmode == "" => in
      case _ => NodeSeq.Empty
    }
  }

  def isDev(in: NodeSeq): NodeSeq = {
    if (Props.devMode) in else NodeSeq.Empty
  }

  def dispatch = {
    case "is" => is
    case "dev" => isDev
  }
}
