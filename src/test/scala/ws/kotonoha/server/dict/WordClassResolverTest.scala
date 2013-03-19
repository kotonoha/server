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

package ws.kotonoha.server.dict

import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import ws.kotonoha.server.test.MongoDb

/**
 * @author eiennohito
 * @since 14.03.13 
 */

class WordClassResolverTest extends FreeSpec with ShouldMatchers with MongoDb {
  "wordclassresolver" - {
    "uma is a simple kun" in {
      val req = DoRecommend("馬","うま")
      WordClassResolver.isSimpleKun(req) should be (true)
    }
  }

}
