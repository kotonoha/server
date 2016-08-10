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

package ws.kotonoha.server.util

import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST.JValue
import net.liftweb.util.Props

/**
  * @author eiennohito
  * @since 2016/08/10
  */
object Json {
  private[this] val minify = !Props.devMode

  def str(jv: JValue): String = {
    if (minify) {
      JsonAST.compactRender(jv)
    } else {
      JsonAST.prettyRender(jv)
    }
  }
}
