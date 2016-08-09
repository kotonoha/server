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

package ws.kotonoha.server.web.snippet

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.http.{DispatchSnippet, StatelessBehavior}
import net.liftweb.util.Props
import ws.kotonoha.akane.resources.Classpath
import ws.kotonoha.akane.utils.XInt
import ws.kotonoha.server.util.BuildInfo
import ws.kotonoha.server.web.loc.NewsId
import ws.kotonoha.server.wiki.WikiRenderer

import scala.xml.NodeSeq

/**
  * @author eiennohito
  * @since 2016/08/02
  */
class News @Inject()(
  p: NewsId
) extends StatelessBehavior with StrictLogging {
  override def statelessDispatch = {
    case "render" => render
  }

  def render(in: NodeSeq): NodeSeq = {
    NewsCache.renderPage(p.id)
  }
}

class NewsItem extends DispatchSnippet with StatelessBehavior {
  import ws.kotonoha.server.web.lift.Binders._

  override def dispatch = {
    case "index" => index
  }

  override def statelessDispatch = dispatch

  def index(x: NodeSeq): NodeSeq = {
    val index = NewsCache.renderPage("index")
    ("* *" #> index).apply(x)
  }
}

object NewsCache {
  val allFiles = BuildInfo.newsMdFiles.filter(x => !x.equals("index.md"))

  val nameRegex = """^(\d{4})/(\d{2})-(\d{2})-([a-zA-Z_]+).md$""".r

  val info: Map[String, NewsItemInfo] = allFiles.collect {
    case o@nameRegex(XInt(year), XInt(month), XInt(day), title) =>
      val date = LocalDate.of(year, month, day)
      NewsItemInfo(
        date,
        title,
        s"/news/$o"
      )
  }.map(i => i.date.toString -> i).toMap + {
    "index" -> NewsItemInfo(
      LocalDate.now(),
      "title-page",
      "/news/index.md"
    )
  }

  def parse(content: String) = WikiRenderer.parseMarkdown(content, "/")

  val pageCache = new ConcurrentHashMap[String, NodeSeq]()

  def renderPage(date: String): NodeSeq = {
    val shouldLoad = !pageCache.contains(date) || Props.devMode
    if (shouldLoad) {
      info.get(date) match {
        case None => <div>Invalid date {date}</div>
        case Some(o) =>
          val content = Classpath.fileAsString(o.path)
          val text = parse(content)
          text
      }
    } else pageCache.get(date)
  }
}

case class NewsItemInfo(
  date: LocalDate,
  name: String,
  path: String
)
