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

package bootstrap.liftweb

import net.liftweb.common.{Empty, Full, Box}
import net.liftweb.http.{Req, S, LiftRules, HtmlProperties}
import java.io.{Writer, InputStream}
import scala.xml.{Node, Elem}
import net.liftweb.util.Html5

/**
 * @author eiennohito
 * @since 07.03.13 
 */

final case class Html5StaticProperties(userAgent: Box[String]) extends HtmlProperties {
  def docType: Box[String] = Empty
  def encoding: Box[String] = Empty

  def contentType: Box[String] = {
      Full("text/html; charset=utf-8")
  }

  def htmlParser: InputStream => Box[Elem] = Html5.parse _

  def htmlWriter: (Node, Writer) => Unit =
    Html5.write(_, _, false, !LiftRules.convertToEntity.vend)

  def htmlOutputHeader: Box[String] = docType.map(_.trim + "\n")

  val html5FormsSupport: Boolean = {
    val r = S.request openOr Req.nil
    r.isSafari5 || r.isFirefox36 || r.isFirefox40 ||
    r.isChrome5 || r.isChrome6
  }

  val maxOpenRequests: Int =
    LiftRules.maxConcurrentRequests.vend(S.request openOr Req.nil)
}
