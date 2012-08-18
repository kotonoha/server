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
import akka.actor.ActorSystem
import ws.kotonoha.server.supermemo.{ItemUpdate, SM6}
import ws.kotonoha.server.records.{ItemLearningDataRecord, WordCardRecord}
import akka.dispatch.Await
import akka.util.Timeout

/**
 * @author eiennohito
 * @since 24.05.12
 */

import ws.kotonoha.server.util.DateTimeUtils.{now => dtNow}
import akka.util.duration._
import akka.pattern.ask

class SM6Test(syst: ActorSystem) extends TestKit(syst) with FreeSpec with ShouldMatchers {

  MongoDbInit.init()
  implicit val timeout: Timeout = 5 minutes

  def this() = this(ActorSystem("test"))

  def withRec[T <: MongoRecord[T]](fact: => T)(f: T => Unit): Unit = {
    val rec = fact
    rec.save
    f(rec)
    rec.delete_!
  }

  "First time learning" - {
    val sm6 = TestActorRef(new SM6(-1L))
    "new cards are graded differently" in {
      withRec(WordCardRecord.createRecord) { card =>
        val o = ItemUpdate(card.learning.is, 1.0, dtNow, -1, card.id.is)
        val m = Await.result((sm6 ? o).mapTo[ItemLearningDataRecord], 5 seconds)
        m.difficulty.is should equal (1.96)
      }
    }
  }
}
