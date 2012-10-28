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

import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoRecord, MongoMetaRecord}
import net.liftweb.mongodb.record.field.{BsonRecordField, ObjectIdPk}
import net.liftweb.record.field.StringField
import net.liftweb.common.{Full, Empty}
import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import com.mongodb.{Mongo, ServerAddress}
import net.liftweb.mongodb.{DefaultMongoIdentifier, MongoDB}

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

class UpdateTest extends FreeSpec with ShouldMatchers {
  "update in lift with bsonrecordfield" - {
    val sa = new ServerAddress("localhost", ServerAddress.defaultPort())
    MongoDB.defineDb(DefaultMongoIdentifier, new Mongo(sa), "tests")
    "works?" in {
      val smt = Smt.createRecord
      smt.save //saved 0 with

      val smt1b = Smt.find(smt.id.is)
      smt1b should not be (Empty)
      val smt1 = smt1b.openOrThrowException("for test")
      smt1.struct(SimpleBsonRecord.createRecord.fld0("test0"))
      smt1.update //calling update first time, with fresh inner bsonrecord
      val smt2b = Smt.find(smt.id.is)
      val inner = smt2b.flatMap(_.struct.valueBox)
      inner should not be (Empty)
      inner.map(_.fld0.is) should be (Full("test0")) //and it is on it's place
      val smt2 = smt2b.openOrThrowException("")
      val str = smt2.struct.is
      str.fld0("cat") //then change a field of inner bsonrecord
      smt2.struct(str)
      smt2.update //and calling update one more time
      val smt3b = Smt.find(smt.id.is)
      //and next line fails with "[test0]" did not equal "[cat]"
      smt3b.openOrThrowException("").struct.is.fld0.is should equal ("cat")
    }
  }
}
