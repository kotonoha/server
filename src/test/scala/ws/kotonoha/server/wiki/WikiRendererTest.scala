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

package ws.kotonoha.server.wiki

import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.server.mongo.MongoAwareTest

/**
 * @author eiennohito
 * @since 20.04.13 
 */

class WikiRendererTest extends FreeSpec with Matchers with MongoAwareTest {



  "WikiRenderer" - {
    "has valid offsite matcher" in {
      val matcher = WikiRenderer.offsite
      matcher.findFirstIn("http://wikipedia.org") should be (Some("http://wikipedia.org"))
    }

    "analyzes urls correctly" in {
      val data = Map(
        "tools" -> false,
        "http://google.com" -> true,
        "tools/extra" -> false,
        "k#words" -> false
      )

      val input = data.keys.toSet
      val analyzed = WikiRenderer.analyzeUrls("main", input)
      for ((k, v) <- analyzed) {
        isExternal(v) shouldBe data(k)
      }
    }
  }

  def isExternal(url: WikiUrl) = url.kind == UrlKind.External

  override protected def beforeAll() = {
    super.beforeAll()

  }

  override protected def afterAll() = {
    super.afterAll()
  }
}
