/*
 * Copyright 2012-2013 eiennohito
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

package ws.kotonoha.server.wiki

import com.tristanhunt.knockoff.{SanitizedXHTMLWriter, XHTMLWriter, Discounter}
import scala.xml.NodeSeq

/**
 * @author eiennohito
 * @since 17.04.13 
 */
object WikiRenderer {

  val parser = new Discounter {}
  val rendrer = new XHTMLWriter with SanitizedXHTMLWriter

  def parseMarkdown(in: String): NodeSeq = {
    val ast = parser.knockoff(in)
    ast.map(rendrer.blockToXHTML(_))
  }
}

