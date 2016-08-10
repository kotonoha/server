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

package ws.kotonoha.server.actors.schedulers

import akka.testkit.TestActorRef
import com.mongodb.casbah.WriteConcern
import net.liftweb.common.Empty
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, FreeSpecLike, Matchers}
import ws.kotonoha.server.mongodb.MongoAwareTest
import ws.kotonoha.server.records.events.NewCardSchedule
import ws.kotonoha.server.records.{UserRecord, UserTagInfo, WordCardRecord}
import ws.kotonoha.server.test.{TestWithAkka, UserTestContext}

/**
 * @author eiennohito
 * @since 06.03.13 
 */

abstract class AkkaFree extends TestWithAkka with FreeSpecLike with Matchers

trait AkkaWithUser extends AkkaFree with MongoAwareTest with BeforeAndAfter {
  protected var uid: ObjectId = null
  protected var usvc: UserTestContext = null
  protected var actor: TestActorRef[NewCardScheduler] = null

  before {
    beforeTest()
  }

  protected def beforeTest(): Unit = {
    uid = createUser()
    usvc = kta.userContext(uid)
    actor = usvc.userActor[NewCardScheduler]("ncs")
  }

  after {
    afterTest()
  }

  protected def afterTest(): Unit = {
    UserRecord.delete("_id", uid)
  }
}

class NewCardSchedulerTest extends AkkaWithUser {
  implicit val sender = testActor
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  import concurrent.duration._

  def createCard(wid: => ObjectId = new ObjectId(), tags: List[String] = Nil) = {
    val card = WordCardRecord.createRecord
    card.word(wid).user(uid).learning(Empty).notBefore(new DateTime).enabled(true).tags(tags)
    card.save(WriteConcern.Safe)
  }

  "newcardscheduler" - {
    "selects 2 cards" in {
      val cards = Seq.fill(2)(createCard())
      actor.receive(Requests.ready(10), testActor)
      val msg = receiveOne(1 second).asInstanceOf[PossibleCards]
      msg.cards should have length (2)
      cleanup(cards)
    }

    "selects 2 cards from 8 when there is a limit on tags" in {
      val empty = Seq.fill(3)(createCard())
      val full = Seq.fill(5)(createCard(tags = List("tag")))
      val ti = UserTagInfo.createRecord.user(uid).tag("tag").limit(2).save(WriteConcern.Safe)
      actor.receive(Requests.ready(10), testActor)
      val msg = receiveOne(1 second).asInstanceOf[PossibleCards]
      msg.cards should have length (5)
      actor.receive(CardsSelected(5))
      val scheds = NewCardSchedule where (_.user eqs uid) and (_.tag eqs "tag") fetch()
      scheds should have length (2)
      cleanup(empty, full, Seq(ti), scheds)
    }

    "makes a right decision about banned tags" in {
      val empty = Seq.fill(3)(createCard())
      val full = Seq.fill(5)(createCard(tags = List("tag1")))
      val ti = UserTagInfo.createRecord.user(uid).tag("tag1").limit(1).save(WriteConcern.Safe)
      actor.receive(Requests.ready(10), testActor)
      val cards = receiveOne(1 second).asInstanceOf[PossibleCards]
      actor.receive(CardsSelected(cards.cards.length))
      val scheds = NewCardSchedule where (_.user eqs uid) and (_.tag eqs "tag1") fetch()
      scheds should have length (1)
      val empty2 = Seq.fill(10)(createCard())
      val und = actor.underlyingActor
      val lims = und.limits()
      val banned = und.bannedTags(lims)
      banned should be (List("tag1"))

      cleanup(empty, full, Seq(ti), empty2)
    }
  }
}
