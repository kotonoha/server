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

import com.tristanhunt.knockoff._
import scala.xml.{Group, Node, NodeSeq}
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import ws.kotonoha.server.records.WikiPageRecord
import net.liftweb.util.Props

object LinkCache {
  import com.foursquare.rogue.LiftRogue._
  /**
   * @param url should begin with /wiki/
   * @return
   */
  def check(url: String): Boolean = {
    if (!url.startsWith("/wiki/")) return false
    val x = cache.get(url)
    if (x == null || Props.devMode) {
        val res = lookupDb(url)
        cache.put(url, res)
        res
    } else x.booleanValue()
  }

  private def lookupDb(path: String): Boolean = {
    val raw = path.substring(6) // remove /wiki/
    val cnt = WikiPageRecord where (_.path eqs raw) limit(1) count()
    cnt != 0
  }

  private val cache = new ConcurrentHashMap[String, java.lang.Boolean]()
}

/**
 * @author eiennohito
 * @since 17.04.13 
 */
object WikiRenderer {

  val parser = new Discounter {}

  val onsite = "(?:[\\p{L}\\p{N}\\\\\\.\\#@\\$%\\+&;\\-_~,\\?=/!]+|\\#(\\w)+)".r

  val offsite = ("\\s*(?:https?://|mailto:)[\\p{L}\\p{N}]" +
        "[\\p{L}\\p{N}\\p{Zs}\\.\\#@\\$%\\+&;:\\-_~,\\?=/!\\(\\)]*\\s*").r

  def handleInternal(base: String, url: String): Option[WikiUrl] = {
    val idx = url.indexOf('#')
    if (idx == -1) {
      val uri = URI.create("/wiki/main")
      val uris = uri.resolve(base).resolve(url).toString
      val kind = if (LinkCache.check(uris)) UrlKind.Internal else UrlKind.Nonexistent
      Some(WikiUrl(url, uris, kind, false))
    } else {
      val end = url.substring(idx + 1)
      val full = url.substring(0, idx) match {
        case "k" => Some("/" + end)
        case _ => None
      }
      full map { proc => WikiUrl(url, proc, UrlKind.Internal, false) }
    }
  }

  def analyzeUrls(base: String, urls: Set[String]) = {
    urls.flatMap {
      case url@offsite(_*) => url -> WikiUrl(url, url, UrlKind.External, true) :: Nil
      case url@onsite(_*) => handleInternal(base, url).map { url -> _ }
      case _ => Nil
    } toMap
  }

  def parseMarkdown(in: String, path: String): NodeSeq = {
    val (links, converter, raw) = parser.knockoffWithLinkDefs(in)
    val sanitizer = new KnockoffSanitizer(converter)
    val ast = sanitizer.sanitize(raw)
    val urls = SpanWalker.spans(ast).flatMap {
      case (x: HaveUrl, _) => x.url :: Nil
      case _ => Nil
    }.toSet

    val urldata = analyzeUrls(path, urls)

    val renderer = new SafeXHTMLWriter(urldata.lift)

    ast.flatMap(renderer.blockToXHTML(_))
  }
}

case class WikiUrl(raw: String, url: String, kind: UrlKind.UrlKind, nofollow: Boolean)

object UrlKind extends Enumeration {
  type UrlKind = Value

  val Internal, Nonexistent, External = Value
}



