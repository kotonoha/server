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

package ws.kotonoha.server.ops

import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.test.AkkaFree

import scala.collection.immutable.BitSet.BitSet1
import scala.concurrent.Await

/**
  * @author eiennohito
  * @since 2016/08/31
  */
class WordOpsSpec extends AkkaFree {
  import scala.concurrent.duration._

  "WordOps" - {
    "bitset of seen examples works" in {
      val user = createUser()
      val wo = kta.userContext(user).ctx.inst[WordOps]
      val record = WordRecord.createRecord.user(user)
      record.save()
      Await.result(wo.markUsedExample(record.id.get, 2), 1.second)
      val loaded = WordRecord.find(record.id.get)
      val bset = new BitSet1(loaded.openOrThrowException("should be present").repExSeen.get)
      bset.toList shouldBe List(2)
    }
  }
}
