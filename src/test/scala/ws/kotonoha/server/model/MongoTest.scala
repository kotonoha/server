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

import ws.kotonoha.server.mongodb.MongoDbInit
import com.foursquare.rogue.Rogue
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter}
import net.liftweb.common.Empty
import akka.util.Timeout
import akka.dispatch.{Future, Await}
import akka.testkit.TestActorRef
import java.util.Calendar
import util.Random
import ws.kotonoha.server.learning.{ProcessMarkEvent, ProcessMarkEvents}
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.server.actors.ReleaseAkkaMain
import ws.kotonoha.server.actors.model.{SchedulePaired, CardActor, RegisterWord}
import ws.kotonoha.server.actors.learning.{WordsAndCards, LoadWords, LoadCards}


trait MongoDb {
  MongoDbInit.init()
}

class MongoTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with BeforeAndAfter
  with BeforeAndAfterAll with MongoDb {
  import akka.pattern._
  import akka.util.duration._
  import ws.kotonoha.server.records._
  import Rogue._

  var user : UserRecord = _
  
  def userId = user.id.is

  implicit val executor = ReleaseAkkaMain.context

  override def beforeAll() {
    user = UserRecord.createRecord.save
    val l = Random.nextLong()
    WordCardRecord.createRecord
    WordRecord.createRecord
  }

  override def afterAll() {
    user.delete_!
    ReleaseAkkaMain.shutdown()
  }

  after {
    WordRecord where (_.user eqs user.id.is) bulkDelete_!!()
    WordCardRecord where (_.user eqs user.id.is) bulkDelete_!!()
  }

  test("saving word for user") {
    val word = WordRecord.createRecord
    val ex1 = ExampleRecord.createRecord.example("this is an example").translation("this is a translation of an example")
    word.writing("example").reading("example").examples(List(ex1))
    word.user(userId)
    word.save
    word.id.valueBox.isEmpty should be (false)

    WordRecord.find(word.id.is).isEmpty should be (false)
  }

  test("card saved with empty learning loads with such") {
    val rec = WordCardRecord.createRecord
    rec.learning(Empty)
    rec.save

    val rec2 = WordCardRecord.find(rec.id.is).openTheBox
    rec2.learning.valueBox.isEmpty should be (true)
  }

  test("card with full learning loads with full too") {
    val rec = WordCardRecord.createRecord
    val l = ItemLearningDataRecord.createRecord
    rec.learning(l)
    rec.save

    val rec2 = WordCardRecord.find(rec.id.is).openTheBox
    rec2.learning.valueBox.isEmpty should be (false)
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
    implicit val timeout = Timeout(500 millis)
    val fut = ReleaseAkkaMain.root ? RegisterWord(createWord)
    fut.mapTo[Long]
  }
  
  def saveWord = {
    val fut = saveWordAsync
    Await.result(fut, 500 milli)
  }

  test("word is being saved all right") {
    val saved = saveWord
    
    val wOpt = WordRecord where (_.writing contains ("例")) and (_.user eqs userId) get()
    wOpt should not be (None)
    val id = wOpt.get.id.is
    saved should equal (id)
    val cards = WordCardRecord where (_.word eqs id) fetch(50)
    cards should have length (2)
    val card = cards.head
    card.learning.valueBox.isEmpty should be (true)
  }

  test ("paired card is being postproned") {
    implicit val system = ReleaseAkkaMain.system
    val id = saveWord    
    val sched = TestActorRef[CardActor]

    val cards = WordCardRecord where (_.word eqs id) fetch()
    cards.length should equal (2)
    val card = cards.head

    sched.receive(SchedulePaired(id, card.cardMode.is))
    val cid = cards.last.id.is
    
    val anotherCard = WordCardRecord.find(cid).get
    anotherCard.notBefore.value should not be (None)
    anotherCard.notBefore.value.get should be >= (Calendar.getInstance)
  }
  
  test("getting new cards and words") {
    implicit val timeout = Timeout(1 second)
    val fs = Future.sequence(1 to 5 map { x => saveWordAsync })
    Await.ready(fs, 5 seconds)

    val wlen = WordRecord where (_.user eqs userId) count()
    wlen should equal (5)
    val clen = WordCardRecord where (_.user eqs userId) count()
    clen should equal (10)
    
    val sel = ask(ReleaseAkkaMain.wordSelector, LoadCards(userId, 6)).mapTo[List[WordCardRecord]]
    val words = Await.result(sel, 1 second)
    words.length should be <= (5)
    val groups = words groupBy { w => w.word.is }
    for ((id, gr) <- groups) {
      gr should have length (1)
    }

    val wicF = ask(ReleaseAkkaMain.wordSelector, LoadWords(userId, 6)).mapTo[WordsAndCards]
    val wic = Await.result(wicF, 2 seconds)
    wic.cards.length should be <= (5)
  }

  test("full work cycle") {
    implicit val timeout : Timeout = 1 minute
    val fs = Future.sequence(1 to 5 map { x => saveWordAsync })
    Await.ready(fs, 150 milli)

    val wicF = ask(ReleaseAkkaMain.wordSelector, LoadWords(userId, 5)).mapTo[WordsAndCards]
    val wic = Await.result(wicF, 5 seconds)

    val card = wic.cards.head
    val event = MarkEventRecord.createRecord
    event.card(card.id.is).mark(5.0).mode(card.cardMode.is).time(2.3142)

    val proc = ReleaseAkkaMain.eventProcessor
    Await.result(ask(proc, ProcessMarkEvents(List(event))), 5 seconds)
    val updatedCard = WordCardRecord.find(card.id.is).get
    updatedCard.learning.valueBox.isEmpty should be (false)
  }
  
  test("Multiple marks for word") {
    import DateTimeUtils._
    implicit val system = ReleaseAkkaMain.system
    implicit val timeout = Timeout(2 seconds)
    val fs = Future.sequence(1 to 2 map { x => saveWordAsync })
    Await.ready(fs, 150 milli)

    val wicF = ask(ReleaseAkkaMain.wordSelector, LoadWords(userId, 5)).mapTo[WordsAndCards]
    val wic = Await.result(wicF, 1 day)

    val card = wic.cards.head
    val event = MarkEventRecord.createRecord
    val cid = card.id.is
    event.card(cid).mark(5.0).mode(card.cardMode.is).datetime(now.withDurationAdded(1 day, 1))

    val ev2 = MarkEventRecord.createRecord
    ev2.card(cid).mark(5.0).mode(card.cardMode.is).datetime(now.withDurationAdded(1 day, 2))

    val ev3 = MarkEventRecord.createRecord
    ev3.card(cid).mark(5.0).mode(card.cardMode.is).datetime(now.withDurationAdded(1 day, 9))

    val ev4 = MarkEventRecord.createRecord
    ev4.card(cid).mark(1.0).mode(card.cardMode.is).datetime(now.withDurationAdded(1 day, 14))


    val proc = ReleaseAkkaMain.eventProcessor
    Await.ready(ask(proc, ProcessMarkEvent(event)), 1 second)
    Await.ready(ask(proc, ProcessMarkEvent(ev2)), 1 second)
    Await.ready(ask(proc, ProcessMarkEvent(ev3)), 1 second)
    Await.ready(ask(proc, ProcessMarkEvent(ev4)), 1 second)

    val updatedCard = WordCardRecord.find(cid).get
    updatedCard.learning.valueBox.isEmpty should be (false)
    updatedCard.learning.value.lapse.is should be (2)
  }
}
