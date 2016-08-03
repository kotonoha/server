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

package ws.kotonoha.server.web.snippet

import com.typesafe.scalalogging.StrictLogging

import xml._
import net.liftweb.http.{DispatchSnippet, S}
import net.liftweb.common.Full
import net.liftweb.util.{Helpers, Props}

/**
 * @author eiennohito
 * @since 31.08.12
 */

/**
 * Classpath resource
 */
object ClasspathResource extends DispatchSnippet {
  def min(in: String) = {
    if (Props.devMode || minattr) {
      in
    } else {
      in + ".min"
    }
  }

  def minattr = S.attr("nomin").flatMap(Helpers.asBoolean).getOrElse(false)

  def js(in: String) = in + ".js"

  def css(in: String): String = in + ".css"

  def minifiedJs(nfo: String): String = {
    js(min(nfo))
  }

  val basepath = "/static/"
  val ngpath = "/static/ng/"
  val jspath = "/static/"

  def script(path: String, minver: Boolean, in: NodeSeq) = {
    S.attr("src") match {
      case Full(x) => {
        val uri = path + (minver match {
          case false => js(x)
          case true => js(min(x))
        })
        <script type="text/javascript" src={uri}></script>
      }
      case _ => Nil
    }
  }

  def css(in: NodeSeq): NodeSeq = {
    S.attr("src") match {
      case Full(x) =>
        <link href={basepath+css(min(x))} type="text/css" rel="stylesheet"></link>
      case _ => Nil
    }
  }

  def dispatch = {
    case "script" => script(basepath, minattr, _)
    case "js" => script(jspath, false, _)
    case "ng" => script(ngpath, false, _)
    case "css" => css
    case x => (ns) => <h1>Error in classpath resource: {x} is not valid</h1>
  }
}

object CdnSnippet extends DispatchSnippet with StrictLogging {
  val isDev = Props.devMode

  override def dispatch = {
    case _ => render
  }

  private def process(s: String): String = {
    if (s.contains(".min")) {
      s
    } else {
      val dot = s.lastIndexOf('.')
      if (dot == -1) {
        s
      } else {
        s"${s.substring(0, dot)}.min${s.substring(dot)}"
      }
    }
  }

  def processAttr(e: Elem, m: MetaData, name: String) = {
    m.get("src") match {
      case Some(Text(s)) =>
        m.append(new UnprefixedAttribute("src", process(s), Null))
      case Some(s) =>
        logger.warn(s"can't process $e, $name attribute is $s of class ${s.getClass}")
        null
      case _ =>
        null
    }
  }

  private def modifySrc(e: Elem): Elem = {
    var md = e.attributes
    val u1 = processAttr(e, md, "src")
    if (u1 != null) {
      md = md.append(u1)
    }
    val u2 = processAttr(e, md, "href")
    if (u2 != null) {
      md = md.append(u2)
    }

    if (md ne e.attributes) {
      e.copy(attributes = md)
    } else e
  }

  def render(ns: NodeSeq): NodeSeq = {
    val need = S.attr("force").isDefined
    ns.map {
      case e: Elem if need || !isDev => modifySrc(e)
      case x => x
    }
  }
}
