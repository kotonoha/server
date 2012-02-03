package org.eiennohito.kotonoha.model

import org.eiennohito.kotonoha.mongodb.MongoDbInit
import com.foursquare.rogue.Rogue
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter}
import org.eiennohito.kotonoha.actors.{RegisterWord, Akka}
import net.liftweb.common.Empty
import org.eiennohito.kotonoha.records._

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

trait MongoDb {
  MongoDbInit.init()
}

class MongoTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with BeforeAndAfter
  with BeforeAndAfterAll with MongoDb {
  import Rogue._

  var user : UserRecord = _
  
  def userId = user.id.is

  override def beforeAll {
    user = UserRecord.createRecord.save
  }

  override def afterAll {
    user.delete_!
    Akka.shutdown()
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
  
  test("word is being saved all right") {
    val rec = WordRecord.createRecord
    val ex = ExampleRecord.createRecord.example("この例はどうにもならんぞ")
    ex.translation("This example is piece of shit!")
    rec.writing("例").reading("れい").meaning("example")
    rec.user(userId).examples(List(ex))
    Akka.wordRegistry ! RegisterWord(rec)
    Thread.sleep(50)
    
    val wOpt = WordRecord where (_.writing eqs "例") and (_.user eqs userId) get()
    wOpt should not be (None)
    val id = wOpt.get.id.is
    val cards = WordCardRecord where (_.word eqs id) fetch(50)
    cards should have length (2)
    val card = cards.head
    card.learning.valueBox.isEmpty should be (true)
  }
}