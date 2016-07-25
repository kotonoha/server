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

package ws.kotonoha.server.gosen

import net.java.sen.SenFactory
import java.util.List
import net.java.sen.dictionary.Token
import scala.collection.JavaConversions._

class TestWorks extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  
  test("it works?") {
    val fact = SenFactory.getStringTagger(null, false)
    val tok: List[Token] = fact.analyze("この世は甘くありませんだろう！")
    tok.foreach { t =>
      val m = t.getMorpheme
      println (t.getSurface)
    }
  }
}
