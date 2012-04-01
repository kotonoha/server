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
import org.eiennohito.kotonoha.records.AppConfig
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.SetHtml

/**
 * @author eiennohito
 * @since 01.04.12
 */

object Config {
  def config(in: NodeSeq): NodeSeq = {
    import net.liftweb.util.Helpers._

    val config = AppConfig.apply()

    def save(): JsCmd = {
      config.save
      SetHtml("status", <b>Saved</b>)
    }

    bind("c", SHtml.ajaxForm(in),
      "invites" -> config.inviteOnly.toForm,
      "uri" -> config.baseUri.toForm,
      "submit" -> SHtml.ajaxSubmit("Save", save)
      )
  }
}
