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

import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.akane.resources.Classpath

/**
  * @author eiennohito
  * @since 2016/08/02
  */
class NewsSnippetSpec extends FreeSpec with Matchers {
  "News snippet cache" - {
    "should have non-empty info" in {
      NewsCache.info should not be empty
    }

    "loaded should be loadable resources" in {
      NewsCache.info.foreach {
        case (_, i) =>
          val res = Classpath.fileAsString(i.path)
          res should not be empty
      }
    }
  }
}
