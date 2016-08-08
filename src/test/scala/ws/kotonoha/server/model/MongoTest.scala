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
package ws.kotonoha.server.model

import akka.actor.Props
import akka.testkit.CallingThreadDispatcher
import akka.util.Timeout
import com.mongodb.WriteConcern
import net.liftweb.common.{Empty, Full}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, OneInstancePerTest}
import ws.kotonoha.server.actors.learning.{LoadCards, LoadWords, WordsAndCards, _}
import ws.kotonoha.server.actors.model.{CardActor, RegisterWord, SchedulePaired}
import ws.kotonoha.server.actors.{AkkaFun, PingUser}
import ws.kotonoha.server.learning.{ProcessMarkEvent, ProcessMarkEvents}
import ws.kotonoha.server.mongo.MongoAwareTest
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.test.UserContext
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.server.util.DateTimeUtils.{now => dtNow}

import scala.concurrent.{Await, Future}


class MongoTest extends AkkaFun with BeforeAndAfter
with BeforeAndAfterAll with MongoAwareTest with OneInstancePerTest {

  import DateTimeUtils._
  import akka.pattern._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.records._

  import concurrent.duration._

  var user: UserRecord = _

  def userId = user.id.get

  var ucont: UserContext = null
  implicit def executor = kta.context

  override def beforeAll() {
    super.beforeAll()
    WordCardRecord.createRecord
    WordRecord.createRecord
  }

  before {
    user = UserRecord.createRecord.save
    ucont = kta.userContext(userId)
    kta ! PingUser(userId)
  }

  after {
    WordRecord where (_.user eqs userId) bulkDelete_!! WriteConcern.NORMAL
    WordCardRecord where (_.user eqs userId) bulkDelete_!! WriteConcern.NORMAL
    user.delete_!
    user = null
  }

  test("saving word for user") {
    val word = WordRecord.createRecord
    val ex1 = ExampleRecord.createRecord.example("this is an example").translation("this is a translation of an example")
    word.writing("example").reading("example").examples(List(ex1))
    word.user(userId)
    val time = dtNow minus (1 day)
    word.createdOn(time)
    word.save(WriteConcern.ACKNOWLEDGED)
    word.id.valueBox.isEmpty should be(false)

    val found = WordRecord.find(word.id.get)
    found.isEmpty should be(false)
    found.flatMap(_.createdOn.valueBox) should be(Full(time))
  }

  test("card saved with empty learning loads with such") {
    val rec = WordCardRecord.createRecord
    rec.learning(Empty)
    rec.save(WriteConcern.ACKNOWLEDGED)

    val rec2 = WordCardRecord.find(rec.id.get).openTheBox
    rec2.learning.valueBox.isEmpty should be(true)
  }

  test("card with full learning loads with full too") {
    val rec = WordCardRecord.createRecord
    val l = ItemLearningDataRecord.createRecord
    rec.learning(l)
    rec.save(WriteConcern.ACKNOWLEDGED)

    val rec2 = WordCardRecord.find(rec.id.get).openOrThrowException("Babah!")
    rec2.learning.valueBox.isEmpty should be(false)
  }

  def createWord = {
    val rec = WordRecord.createRecord
    val ex = ExampleRecord.createRecord.example("この例はどうにもならんぞ")
    ex.translation("This example is piece of shit!")
    rec.writing("例").reading("れい").meaning("example")
    rec.user(userId).examples(List(ex))
    rec.status(WordStatus.Approved)
    rec
  }

  def saveWordAsync = {
    implicit val timeout = Timeout(2500 millis)
    val fut = ucont.actor ? RegisterWord(createWord)
    fut.mapTo[ObjectId]
  }

  def saveWord = {
    val fut = saveWordAsync
    Await.result(fut, 2500 milli)
  }

  test("word is being saved all right") {
    val saved = saveWord

    val wOpt = WordRecord where (_.writing contains ("例")) and (_.user eqs userId) get()
    wOpt should not be (None)
    val id = wOpt.get.id.get
    saved should equal(id)
    val cards = WordCardRecord where (_.word eqs id) fetch (50)
    cards should have length (2)
    val card = cards.head
    card.learning.valueBox.isEmpty should be(true)
  }

  test("paired card is being postproned") {
    val id = saveWord
    val cards = WordCardRecord where (_.word eqs id) fetch()

    cards.length should equal(2)
    val card = cards.head

    val sched = ucont.userActor(Props[CardActor].withDispatcher(CallingThreadDispatcher.Id), "ca")
    sched.receive(SchedulePaired(id, card.cardMode.get))
    val cid = cards.last.id.get

    val anotherCard = WordCardRecord.find(cid).openOrThrowException("okay")
    anotherCard.notBefore.value should not be (None)
    anotherCard.notBefore.get should be >= (new DateTime)
  }

  test("getting new cards from actor") {
    val fs = Future.sequence(1 to 5 map {
      x => saveWordAsync
    })
    val ids = Await.result(fs, 5 seconds)
    ids should have size (5)

    val ar = kta.userContext(userId).userActor[CardLoader]("usa")
    ar.receive(LoadNewCards(userId, 10), testActor)
    val cards = receiveOne(1.minute).asInstanceOf[List[WordCardRecord]]
    cards should have length (10)
  }

  test("getting new cards and words") {
    val fs = Future.sequence(1 to 5 map {
      x => saveWordAsync
    })
    Await.result(fs, 5 seconds)

    val wlen = WordRecord where (_.user eqs userId) count()
    wlen should equal(5)
    val clen = WordCardRecord where (_.user eqs userId) count()
    clen should equal(10)

    val sel = ask(ucont.actor, LoadCards(6, 0)).mapTo[WordsAndCards]
    val wac = Await.result(sel, 1 second)
    val cards = wac.cards
    cards.length should be >= (1)
    val groups = cards groupBy {
      w => w.word.get
    }
    for ((id, gr) <- groups) {
      gr should have length (1)
    }

    val wicF = ask(ucont.actor, LoadWords(6, 0)).mapTo[WordsAndCards]
    val wic = Await.result(wicF, 2 seconds)
    wic.cards.length should be >= (1)
  }

  test("full work cycle") {
    val fs = Future.sequence(1 to 5 map {
      _ => saveWordAsync
    })
    Await.result(fs, 2.seconds)

    val wicF = ask(ucont.actor, LoadWords(5, 0)).mapTo[WordsAndCards]
    val wic = Await.result(wicF, 5 seconds)

    val card = wic.cards.head
    val event = MarkEventRecord.createRecord
    event.card(card.id.get).mark(5.0).mode(card.cardMode.get).time(2.3142)
    event.user(userId)

    Await.result(ask(ucont.actor, ProcessMarkEvents(List(event))), 5 seconds)
    val updatedCard = WordCardRecord.find(card.id.get).openOrThrowException("ok")
    updatedCard.learning.valueBox.isEmpty should be(false)
  }

  test("Multiple marks for word") {
    import DateTimeUtils._
    implicit val timeout = Timeout(2 seconds)
    val fs = Future.sequence(1 to 2 map {
      x => saveWordAsync
    })
    Await.result(fs, 1500 milli)

    (ucont.actor ! LoadWords(5, 0))(testActor)
    val wic = receiveOne(5 minutes).asInstanceOf[WordsAndCards]

    val card = wic.cards.head
    val event = MarkEventRecord.createRecord
    val cid = card.id.get
    event.card(cid).mark(5.0).mode(card.cardMode.get).datetime(dtNow.withDurationAdded(1 day, 0))
    event.user(userId)


    val ev2 = MarkEventRecord.createRecord
    ev2.card(cid).mark(5.0).mode(card.cardMode.get).datetime(dtNow.withDurationAdded(1 day, 2))
    ev2.user(userId)

    val ev3 = MarkEventRecord.createRecord
    ev3.card(cid).mark(5.0).mode(card.cardMode.get).datetime(dtNow.withDurationAdded(1 day, 9))
    ev3.user(userId)

    val ev4 = MarkEventRecord.createRecord
    ev4.card(cid).mark(1.0).mode(card.cardMode.get).datetime(dtNow.withDurationAdded(1 day, 14))
    ev4.user(userId)


    val proc = ucont.actor
    implicit val ar = testActor

    proc ! ProcessMarkEvent(event)
    receiveOne(1 minute)
    proc ! ProcessMarkEvent(ev2)
    receiveOne(1 minute)
    proc ! ProcessMarkEvent(ev3)
    receiveOne(1 minute)
    proc ! ProcessMarkEvent(ev4)
    receiveOne(1 minute)

    val updatedCard = WordCardRecord.find(cid).openOrThrowException("okay")
    updatedCard.learning.valueBox.isEmpty should be(false)
    updatedCard.learning.value.lapse.get should be(2)
  }
}
