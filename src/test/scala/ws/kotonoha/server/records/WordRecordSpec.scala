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

package ws.kotonoha.server.records

import net.liftweb.common.Empty
import net.liftweb.json.JsonAST.JNothing
import ws.kotonoha.server.test.AkkaFree

/**
  * @author eiennohito
  * @since 2016/08/23
  */
class WordRecordSpec extends AkkaFree {
  "WordRecord" - {
    "should have empty repExamples by default" in {
      val rec = WordRecord.createRecord
      rec.repExamples.valueBox shouldBe Empty
    }

    "should not have repExamples in dbObject or bson object by default" in {
      val rec = WordRecord.createRecord
      val dbo = rec.asDBObject
      dbo.get(rec.repExamples.name) shouldBe null
      val bson = WordRecord.toRMong(rec)
      bson.get(rec.repExamples.name) shouldBe None
      val jv = rec.asJValue
      jv \ rec.repExamples.name shouldBe JNothing
    }
  }
}
