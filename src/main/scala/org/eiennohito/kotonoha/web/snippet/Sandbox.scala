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

import net.liftweb.http.SHtml

/**
 * @author eiennohito
 * @since 29.04.12
 */

object Sandbox {
  import net.liftweb.util.Helpers._
  val list = List("1" -> "1", "2" -> "2", "3" -> "3")
  def objects = {
    def selected(in: Seq[String]) = {
      in.foreach{println(_)}
    }

    val holder = SHtml.checkbox[String](List("1", "2", "5", "hatered!"), Nil, println(_))
    "li" #> SHtml.multiSelect(list, 1 to 3 map (_.toString), selected(_)) &
    ".cb" #> holder.toForm
  }
}
