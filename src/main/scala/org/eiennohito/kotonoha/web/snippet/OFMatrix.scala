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
import net.liftweb.http.js.JsCmds.Script
import org.eiennohito.kotonoha.web.ajax.AllJsonHandler
import org.eiennohito.kotonoha.utls.Snippets._

/**
 * @author eiennohito
 * @since 06.03.12
 */

object OFMatrix {

  val loadFncName = "loadOFMatrix"

  def dataScript(in: NodeSeq): NodeSeq = Script(AllJsonHandler.is.jsCmd & callbackFn(loadFncName))

}
