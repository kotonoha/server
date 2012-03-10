package org.eiennohito.kotonoha.util

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

import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Function
import net.liftweb.http.js.JE.{JsObj, JsVar}
import org.eiennohito.kotonoha.web.ajax.AllJsonHandler

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
/**
 * @author eiennohito
 * @since 06.03.12
 */

object Snippets {

  object XString {
    def unapply(in: Any): Option[String] = in match {
      case s: String => Some(s)
      case _ => None
    }
  }

  object XArrayNum {
    def unapply(in: Any): Option[List[Number]] =
      in match {
        case lst: List[_] => Some(lst.flatMap {
          case n: Number => Some(n)
          case _ => None
        })
        case _ => None
      }
  }


  def callbackFn(name: String): JsCmd = {
    Function(name, List("callback"),
      AllJsonHandler.is.call(name,
        JsVar("callback"),
        JsObj()))
  }
}
