/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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
import net.liftweb.http.{DispatchSnippet, LiftSession, S}

import scala.xml.{Elem, NodeSeq}
import net.liftweb.util.Helpers._

/**
  * @author eiennohito
  * @since 2016/08/03
  */
class MenuPostproc @Inject() (
  ls: LiftSession
) extends DispatchSnippet {

  val tf1 = "[class]" #> "active"
  val tf2 = "li.active [class!]" #> "active"

  private def addActive(ns: NodeSeq): NodeSeq = {
    ns match {
      case e: Elem if e.text == "li" =>
        val as = e.child.flatMap {
          case e: Elem if e.text == "a" => tf1(e)
          case y => y.theSeq
        }
        tf2(e).asInstanceOf[Elem].copy(child = as)
      case x => x
    }
  }

  def render(in: NodeSeq): NodeSeq = {
    val tf =
      ".nav-item a [class]" #> "nav-link" &
      ".active" #> { addActive _ }
    tf.apply(in)
  }

  override def dispatch = {
    case _ => render
  }
}
