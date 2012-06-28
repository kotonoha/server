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

package org.eiennohito.kotonoha.util

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FreeSpec
import java.util
import net.liftweb.json.JsonAST.{JValue, JObject}

/**
 * @author eiennohito
 * @since 27.06.12
 */

class KBsonDSLTest extends FreeSpec with ShouldMatchers {
  import KBsonDSL._
  val d = new util.Date()
  "kbsondsl" - {
    "date is okay" in {
      val jv: JObject = "date" -> d
    }
  }
}
