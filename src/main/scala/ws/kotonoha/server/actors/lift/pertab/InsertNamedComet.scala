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

package com.fmpwizard.cometactor.pertab
package namedactor

import net.liftweb.common.Full
import net.liftweb.http.{DispatchSnippet, S, SessionVar}

import scala.xml.NodeSeq


/**
 * This trait adds a named comet actor on the page.
 * You should at least override cometClass to match the
 * class of your comet actor
 *
 * If all you want is different comet actors per tab, do not override
 * lazy val name.
 * If you want the same actor on some of the tabs, and you have a
 * specific value to group them, override lazy val name
 * override lazy val name= S.param("q")
 *
 *
 */
trait InsertNamedComet extends DispatchSnippet { self =>
  /**
   * These are the two val(s) you would have to
   * override after extending this trait.
   * No need to touch the render method (I hope)
   */
  def cometClass: String

  private lazy val savedName = new SessionVar[String](net.liftweb.util.Helpers.nextFuncName) {
    override protected def __nameSalt = self.getClass.getName
  }

  def name = savedName.get

  def messages = List[AnyRef]()

  def enabled = true


  override def dispatch =  {
    case "render" => render
  }

  final def render(xhtml: NodeSeq): NodeSeq = {
    if (enabled) {
      for (sess <- S.session) {
        messages foreach (
          sess.sendCometActorMessage(cometClass, Full(name), _)
          )
      }
      <lift:comet type={cometClass} name={name}>{xhtml}</lift:comet>
    } else {
      NodeSeq.Empty
    }
  }
}
