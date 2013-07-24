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

import org.scalatest.{FunSuite, BeforeAndAfterAll, BeforeAndAfter}
import net.liftweb.common.Empty
import akka.util.Timeout
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.server.util.DateTimeUtils.{now => dtNow}
import ws.kotonoha.server.actors.model.CardActor
import ws.kotonoha.server.actors.learning._
import org.bson.types.ObjectId
import org.scalatest.matchers.ShouldMatchers
import ws.kotonoha.server.test.TestWithAkka
import akka.actor.Props
import akka.testkit.CallingThreadDispatcher
import com.mongodb.WriteConcern
import concurrent.{Future, Await}
import org.joda.time.DateTime
import ws.kotonoha.server.actors.learning.LoadWords
import ws.kotonoha.server.learning.ProcessMarkEvents
import net.liftweb.common.Full
import ws.kotonoha.server.learning.ProcessMarkEvent
import ws.kotonoha.server.actors.model.SchedulePaired
import ws.kotonoha.server.actors.learning.LoadCards
import ws.kotonoha.server.actors.learning.WordsAndCards
import ws.kotonoha.server.actors.model.RegisterWord
import ws.kotonoha.server.actors.PingUser
import ws.kotonoha.server.records.events.MarkEventRecord


class MongoTest extends TestWithAkka with FunSuite with ShouldMatchers with BeforeAndAfter
with BeforeAndAfterAll {

  import akka.pattern._
  import concurrent.duration._
  import ws.kotonoha.server.records._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import DateTimeUtils._

  var user: UserRecord = _

  def userId = user.id.is

  lazy val ucont = kta.userContext(userId)
  implicit val executor = kta.context

  override def beforeAll() {
    user = UserRecord.createRecord.save
    WordCardRecord.createRecord
    WordRecord.createRecord
  }

  override def afterAll() {
    user.delete_!
  }

  before {
    kta ! PingUser(userId)
  }

  after {
    WordRecord where (_.user eqs user.id.is) bulkDelete_!! (WriteConcern.NORMAL)
    WordCardRecord where (_.user eqs user.id.is) bulkDelete_!! (WriteConcern.NORMAL)
  }

  test("saving word for user") {
    val word = WordRecord.createRecord
    val ex1 = ExampleRecord.createRecord.example("this is an example").translation("this is a translation of an example")
    word.writing("example").reading("example").examples(List(ex1))
    word.user(userId)
    val time = dtNow minus (1 day)
    word.createdOn(time)
    word.save
    word.id.valueBox.isEmpty should be(false)

    val found = WordRecord.find(word.id.is)
    found.isEmpty should be(false)
    found.flatMap(_.createdOn.valueBox) should be(Full(time))
  }

  test("card saved with empty learning loads with such") {
    val rec = WordCardRecord.createRecord
    rec.learning(Empty)
    rec.save

    val rec2 = WordCardRecord.find(rec.id.is).openTheBox
    rec2.learning.valueBox.isEmpty should be(true)
  }

  test("card with full learning loads with full too") {
    val rec = WordCardRecord.createRecord
    val l = ItemLearningDataRecord.createRecord
    rec.learning(l)
    rec.save

    val rec2 = WordCardRecord.find(rec.id.is).openOrThrowException("Babah!")
    rec2.learning.valueBox.isEmpty should be(false)
  }

  def createWord = {
    val rec = WordRecord.createRecord
    val ex = ExampleRecord.createRecord.example("この例はどうにもならんぞ")
    ex.translation("This example is piece of shit!")
    rec.writing("例").reading("れい").meaning("example")
    rec.user(userId).examples(List(ex))
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
    val id = wOpt.get.id.is
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
    sched.receive(SchedulePaired(id, card.cardMode.is))
    val cid = cards.last.id.is

    val anotherCard = WordCardRecord.find(cid).get
    anotherCard.notBefore.value should not be (None)
    anotherCard.notBefore.is should be >= (new DateTime)
  }

  test("getting new cards from actor") {
    val fs = Future.sequence(1 to 5 map {
      x => saveWordAsync
    })
    Await.ready(fs, 5 seconds)

    val ar = kta.userContext(userId).userActor[CardLoader]("usa")
    ar.receive(LoadNewCards(userId, 10), testActor)
    val cards = receiveOne(1 minute).asInstanceOf[List[WordCardRecord]]
    cards should have length (10)
  }

  test("getting new cards and words") {
    val fs = Future.sequence(1 to 5 map {
      x => saveWordAsync
    })
    Await.ready(fs, 5 seconds)

    val wlen = WordRecord where (_.user eqs userId) count()
    wlen should equal(5)
    val clen = WordCardRecord where (_.user eqs userId) count()
    clen should equal(10)

    val sel = ask(ucont.actor, LoadCards(6, 0)).mapTo[List[WordCardRecord]]
    val words = Await.result(sel, 1 second)
    words.length should be >= (1)
    val groups = words groupBy {
      w => w.word.is
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
      x => saveWordAsync
    })
    Await.ready(fs, 1000 milli)

    val wicF = ask(ucont.actor, LoadWords(5, 0)).mapTo[WordsAndCards]
    val wic = Await.result(wicF, 5 seconds)

    val card = wic.cards.head
    val event = MarkEventRecord.createRecord
    event.card(card.id.is).mark(5.0).mode(card.cardMode.is).time(2.3142)
    event.user(userId)

    Await.ready(ask(ucont.actor, ProcessMarkEvents(List(event))), 5 seconds)
    val updatedCard = WordCardRecord.find(card.id.is).get
    updatedCard.learning.valueBox.isEmpty should be(false)
  }

  test("Multiple marks for word") {
    import DateTimeUtils._
    implicit val timeout = Timeout(2 seconds)
    val fs = Future.sequence(1 to 2 map {
      x => saveWordAsync
    })
    Await.ready(fs, 1500 milli)

    (ucont.actor ! LoadWords(5, 0))(testActor)
    val wic = receiveOne(5 minutes).asInstanceOf[WordsAndCards]

    val card = wic.cards.head
    val event = MarkEventRecord.createRecord
    val cid = card.id.is
    event.card(cid).mark(5.0).mode(card.cardMode.is).datetime(dtNow.withDurationAdded(1 day, 0))
    event.user(userId)


    val ev2 = MarkEventRecord.createRecord
    ev2.card(cid).mark(5.0).mode(card.cardMode.is).datetime(dtNow.withDurationAdded(1 day, 2))
    ev2.user(userId)

    val ev3 = MarkEventRecord.createRecord
    ev3.card(cid).mark(5.0).mode(card.cardMode.is).datetime(dtNow.withDurationAdded(1 day, 9))
    ev3.user(userId)

    val ev4 = MarkEventRecord.createRecord
    ev4.card(cid).mark(1.0).mode(card.cardMode.is).datetime(dtNow.withDurationAdded(1 day, 14))
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

    val updatedCard = WordCardRecord.find(cid).get
    updatedCard.learning.valueBox.isEmpty should be(false)
    updatedCard.learning.value.lapse.is should be(2)
  }
}
