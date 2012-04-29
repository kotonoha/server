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

import xml.NodeSeq
import net.liftweb.http.js.{JsCmd, JsExp, JE, JsonCall}
import net.liftweb.http.js.JsCmds.{RedirectTo, SetHtml, _Noop}
import net.liftweb.http._
import net.liftweb.common.Full

/**
 * @author eiennohito
 * @since 17.03.12
 */

object AddWord {
  import net.liftweb.util.Helpers._
  def addField(in: NodeSeq): NodeSeq = {
    var data = ""

    def process {
      val d = data
      val i = 0
      //S.redirectTo()
      //RedirectWithState("added")
    }

    bind("word", SHtml.ajaxForm(in),
      "data" -> SHtml.textarea(data, data = _),
      "submit" -> SHtml.submit("Add words", process _))
  }

  def anotherSnippet(in: NodeSeq): NodeSeq = {
    S.runTemplate("templates-hidden" :: "test" :: Nil) match {
      case Full(x) => x
      case _ => <em>Error in rendering template test</em>
    }
  }
}


class WordActor extends CometActor {
  def render = null

  def doSmt = {
    partialUpdate(JE.Call("smt", JE.Str("1")).cmd)
  }
}
