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

package ws.kotonoha.server.mongo

import com.mongodb.MongoClient
import com.mongodb.casbah.WriteConcern
import net.liftweb.common.{Empty, Full}
import net.liftweb.mongodb.MongoDB
import net.liftweb.mongodb.record.field.{BsonRecordField, ObjectIdPk}
import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.StringField
import net.liftweb.util.DefaultConnectionIdentifier
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers, Suite}
import ws.kotonoha.server.mongodb.MongoDbInit

/**
 * @author eiennohito
 * @since 28.10.12 
 */

class SimpleBsonRecord extends BsonRecord[SimpleBsonRecord] {
  def meta = SimpleBsonRecord

  object fld0 extends StringField(this, 100)
}

object SimpleBsonRecord extends SimpleBsonRecord with BsonMetaRecord[SimpleBsonRecord]

class Smt private() extends MongoRecord[Smt] with ObjectIdPk[Smt] {
  def meta = Smt

  object struct extends BsonRecordField(this, SimpleBsonRecord, Empty)
}

object Smt extends Smt with MongoMetaRecord[Smt]

trait MongoAwareTest extends Suite with BeforeAndAfterAll { self: Suite =>
  protected var mongoClient: MongoClient = null

  abstract override protected def beforeAll() = {
    mongoClient = new MongoClient("localhost")
    MongoDB.defineDb(DefaultConnectionIdentifier, mongoClient, "tests")
    MongoDbInit.register(mongoClient, "kototests", "kotodictests")
    mongoClient.fsync(false)
    super.beforeAll()
  }

  abstract override protected def afterAll() = {
    super.afterAll()
    mongoClient.close()
    mongoClient = null
  }
}

class UpdateTest extends FreeSpec with Matchers with MongoAwareTest {
  "update in lift with bsonrecordfield" - {
    "works?" in {
      val smt: Smt = Smt.createRecord
      smt.save(safe = true) //saved 0 with

      val smt1b = Smt.find(smt.id.get)
      smt1b should not be (Empty)
      val smt1 = smt1b.openOrThrowException("for test")
      smt1.struct(SimpleBsonRecord.createRecord.fld0("test0"))
      smt1.update //calling update first time, with fresh inner bsonrecord
      val smt2b = Smt.find(smt.id.get)
      val inner = smt2b.flatMap(_.struct.valueBox)
      inner should not be (Empty)
      inner.map(_.fld0.get) should be (Full("test0")) //and it is on it's place
      val smt2 = smt2b.openOrThrowException("")
      val str = smt2.struct.get
      str.fld0("cat") //then change a field of inner bsonrecord
      smt2.struct(str)
      smt2.save(WriteConcern.Acknowledged) //and calling update one more time
      val smt3b = Smt.find(smt.id.get)
      //and next line fails with "[test0]" did not equal "[cat]"
      smt3b.openOrThrowException("").struct.get.fld0.get should equal ("cat")
    }
  }
}
