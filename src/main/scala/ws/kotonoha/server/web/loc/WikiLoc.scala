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

package ws.kotonoha.server.web.loc

import net.liftweb.common.Full
import net.liftweb.http.{ParsePath, RewriteRequest, RewriteResponse}
import net.liftweb.sitemap.Loc
import net.liftweb.sitemap.Loc.{Hidden, Link}
import ws.kotonoha.server.records.UserRecord

/**
 * @author eiennohito
 * @since 29.01.13 
 */
case class WikiPage(path: List[String]) {
  def mkPath = path.mkString("/")
}

class WikiLoc extends Loc[WikiPage] {
  def name = "WikiRenderInternal"

  def link = new Link[WikiPage]("wiki-page" :: Nil) {
  }

  def text = "Wiki"

  def defaultValue = Full(WikiPage("main" :: Nil))

  def params = Hidden :: Nil

  override def rewrite: LocRewrite = Full({
    case RewriteRequest(ParsePath("wiki" :: lst, _, _, _), _, _) => {
      lst match {
        case Nil => RewriteResponse("wiki-page" :: Nil, true) -> WikiPage("main" :: Nil)
        case _ => RewriteResponse("wiki-page" :: Nil, true) -> WikiPage(lst)
      }
    }
  })

  override def stateless_? = UserRecord.haveUser
}
