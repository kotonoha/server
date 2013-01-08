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

import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import ws.kotonoha.server.mongodb.MongoDbInit
import net.liftweb.mongodb.record.MongoRecord
import akka.testkit.{TestKit, TestActorRef}
import akka.actor.{Props, ActorSystem}
import ws.kotonoha.server.supermemo.{ProcessMark, SM6}
import ws.kotonoha.server.records.{ItemLearningDataRecord, WordCardRecord}
import akka.dispatch.Await
import akka.util.Timeout
import org.bson.types.ObjectId
import ws.kotonoha.server.test.{KotonohaTestAkka, TestWithAkka}

/**
 * @author eiennohito
 * @since 24.05.12
 */

import ws.kotonoha.server.util.DateTimeUtils.{now => dtNow}
import akka.util.duration._
import akka.pattern.ask

class SM6Test extends TestWithAkka with FreeSpec with ShouldMatchers {
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
