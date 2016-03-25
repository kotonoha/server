/*
 * Copyright 2012-2013 eiennohito
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

import org.scalatest.{BeforeAndAfterAll, FreeSpec}
import org.scalatest.matchers.ShouldMatchers
import ws.kotonoha.server.test.TestWithAkka
import ws.kotonoha.server.records.{ItemLearningDataRecord, WordCardRecord, UserRecord}

/**
 * @author eiennohito
 * @since 27.02.13 
 */

class RepetitionStateResolverTest extends AkkaFree with BeforeAndAfterAll {

  import ws.kotonoha.server.util.DateTimeUtils.{now => dtNow, _}

  override def afterAll() {
    user.delete_!
  }

  val user = {
    UserRecord.createRecord.save
  }

  def uid = user.id.is

  def registerCards() = {
    (1 to 5) foreach {
      i =>
        val rec = WordCardRecord.createRecord
        rec.user(uid)
        val lrn = ItemLearningDataRecord.createRecord
        lrn.intervalEnd(dtNow.plusDays(i))
        rec.learning(lrn)
        rec.enabled(true)
        rec.save
    }
  }

  "RepetitionStateResolver" - {
    "don't throw exceptions in next method" in {
      registerCards()
      val res = new RepetitionStateResolver(uid)
      println(res.next)
    }
  }

}
