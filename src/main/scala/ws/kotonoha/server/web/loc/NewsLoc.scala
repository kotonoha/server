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

package ws.kotonoha.server.web.loc

import net.liftweb.common.Full
import net.liftweb.http.{ParsePath, RewriteRequest, RewriteResponse}
import net.liftweb.sitemap.Loc
import net.liftweb.sitemap.Loc.{Hidden, Link}
import ws.kotonoha.server.web.snippet.NewsCache

/**
  * @author eiennohito
  * @since 2016/08/02
  */
class NewsLoc extends Loc[NewsId] {
  override def name = "NewsItemRender"

  override def link = new Link(List("news"))

  override def text = "News"

  override def defaultValue = Full(NewsId("index"))

  override def params = Hidden :: Nil

  override def rewrite = Full({
    case RewriteRequest(ParsePath("news" :: date :: _, _, _, _), _, _) if NewsCache.info.contains(date) =>
      RewriteResponse(List("news")) -> Full(NewsId(date))
  })

  override def stateless_? = true
}

case class NewsId(id: String)
