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

package ws.kotonoha.server.lift

import xml.Text


class CssTransformTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  import net.liftweb.util.Helpers._

  test("smt") {
    val xml = <div><span id="what"><a>inner</a><b>whatever</b></span></div>
    val transf = "#what *" #> { x => { println(x); Text("inner") }}
    val obj = transf(xml)
    println(obj)
  }

  test("adding id") {
    val xml = <input></input>
    val tf = "input [id]" #> "some"
    val tfed = tf(xml)
    println(tfed)
  }

}
