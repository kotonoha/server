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

package ws.kotonoha.server.learning

import akka.actor.Props
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.schedulers.AkkaFree
import ws.kotonoha.server.records.{ItemLearningDataRecord, WordCardRecord}
import ws.kotonoha.server.supermemo.{ProcessMark, SM6}

import scala.concurrent.Await

/**
 * @author eiennohito
 * @since 24.05.12
 */

import akka.pattern.ask
import ws.kotonoha.server.util.DateTimeUtils.{now => dtNow}

import scala.concurrent.duration._

class SM6Test extends AkkaFree {
  "First time learning" - {
    val id = new ObjectId()
    val sm6 = kta.userContext(id).userActor(Props[SM6], "sm6")
    "new cards are graded differently" in {
      withRec(WordCardRecord.createRecord) { card =>
        val o = ProcessMark(card.learning.is, 1.0, dtNow, id, card.id.is)
        val m = Await.result((sm6 ? o).mapTo[ItemLearningDataRecord], 5 seconds)
        m.difficulty.is should equal (1.96)
      }
    }
  }
}
