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
import net.liftweb.http.{CometActor, SHtml}
import net.liftweb.http.js.{JE, JsonCall}

/**
 * @author eiennohito
 * @since 17.03.12
 */

object AddWord {
  def addField(in: NodeSeq): NodeSeq = {
    in
  }
}


class WordActor extends CometActor {
  def render = null

  def doSmt = {
    partialUpdate(JE.Call("smt", JE.Str("1")).cmd)
  }

  def foo[T](bar: => T) = bar

  val foobar = foo {/*enter and tab doesn't work here*/}
}
