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
import com.fmpwizard.cometactor.pertab.namedactor.InsertNamedComet
import org.eiennohito.kotonoha.util.unapply.XLong
import org.eiennohito.kotonoha.web.comet.{PrepareWords, WordList}
import org.eiennohito.kotonoha.records.{AddWordRecord, UserRecord}
import util.Random
import com.weiglewilczek.slf4s.Logging

/**
 * @author eiennohito
 * @since 17.03.12
 */

object AddWord extends Logging {
  import net.liftweb.util.Helpers._
  def addField(in: NodeSeq): NodeSeq = {
    var data = ""

    def process = {
      logger.debug("trying to add words from string " + data)
      val d = data.split("\n").map(_.trim).filter(_.length != 0)
      val opid = Random.nextLong()
      d.map(d => AddWordRecord.createRecord.content(d).processed(false).group(opid).user(UserRecord.currentId)).foreach(_.save)
      RedirectTo("/words/approve_added?list="+opid.toHexString)
      //SetHtml("asdf", <a href="http://google.com">Google</a>)
    }

    bind("word", SHtml.ajaxForm(in),
      "data" -> SHtml.textarea(data, data = _),
      "submit" -> SHtml.ajaxSubmit("Add words", () => process))
  }

  def anotherSnippet(in: NodeSeq): NodeSeq = {
    S.runTemplate("templates-hidden" :: "test" :: Nil) match {
      case Full(x) => x
      case _ => <em>Error in rendering template test</em>
    }
  }
}

object AddWordActorSnippet extends InsertNamedComet {

  /**
   * These are the two val(s) you would have to
   * override after extending this trait.
   * No need to touch the render method (I hope)
   */
  def cometClass = "AddWordActor"

  override def name = {
    S.param("list") match {
      case Full(XLong(lid)) => "list"+lid
      case _ => "user" + UserRecord.currentUserId.openOr(super.name)
    }
  }

  override def messages = {
    S.param("list") match {
      case Full(XLong(id)) => WordList(id) :: Nil
      case _ => PrepareWords :: Nil
    }
  }
}
