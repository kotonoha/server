package org.eiennohito.kotonoha.model

import org.scalatest.BeforeAndAfter
import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.records.{ExampleRecord, WordRecord, UserRecord}

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

class MongoTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with BeforeAndAfter with MongoDb {

  var user : UserRecord = _

  before {
    val all = UserRecord.findAll
    if (all.length == 0) {
      user = UserRecord.createRecord
      user.save
    } else {
      user = all.head
    }
  }

  test("saving word for user") {
    val word = WordRecord.createRecord
    val ex1 = ExampleRecord.createRecord.example("this is an example").translation("this is a translation of an example")
    word.writing("example").reading("example").examples(List(ex1)).user(user.id.is)
    word.save
    word.id.valueBox.isEmpty should be (false)
  }

}