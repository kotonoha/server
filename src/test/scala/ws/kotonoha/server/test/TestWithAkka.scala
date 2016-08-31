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

package ws.kotonoha.server.test

import akka.testkit.{TestKit, TestProbe}
import akka.util.Timeout
import com.mongodb.casbah.WriteConcern
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.mongodb.record.MongoRecord
import org.bson.types.ObjectId
import org.joda.time.DateTime
import ws.kotonoha.server.ioc.Res
import ws.kotonoha.server.mongodb.MongoAwareTest
import ws.kotonoha.server.records.UserRecord

import scala.concurrent.duration._
import scala.util.Random

/**
 * @author eiennohito
 * @since 08.01.13
 */
abstract class TestWithAkka(protected val kta: KotonohaTestAkka = new KotonohaTestAkka) extends TestKit(kta.system) with MongoAwareTest with StrictLogging {
  implicit val timeout: Timeout = 5 minutes

  def withRec[T <: MongoRecord[T]](fact: => T)(f: T => Unit): Unit = {
    val rec = fact
    rec.save(WriteConcern.Acknowledged)
    f(rec)
    rec.delete_!
  }

  def createUser(): ObjectId = {
    val user = UserRecord.createRecord
    user.username("test" + new DateTime() + Random.nextLong().toHexString)
    user.save(WriteConcern.Acknowledged)
    user.id.get
  }

  def cleanup(seqs: TraversableOnce[MongoRecord[_]]*) = {
    seqs.flatten.foreach(_.delete_!)
  }

  def probe = TestProbe()(kta.system)

  def inst[T: Manifest] = kta.ioc.inst[T]


  override protected def beforeAll() = {
    super.beforeAll()
  }

  override protected def afterAll() = {
    super.afterAll()
    kta.inj.getInstance(classOf[Res]).close()
  }
}
