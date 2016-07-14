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

package ws.kotonoha.server.mongo.mapreduce

import com.mongodb.WriteConcern
import net.liftweb.json.DefaultFormats
import org.bson.types.ObjectId
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.server.mongo.MongoAwareTest
import ws.kotonoha.server.mongodb.mapreduce.LearningStats
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.util.DateTimeUtils

import scala.util.Random


class LearningStatsTest extends FreeSpec with Matchers with MongoAwareTest {
  implicit def formats = DefaultFormats

  def createSomeData() = {
    val uid = new ObjectId()

    val op = MarkEventRecord.useColl( c => c.initializeUnorderedBulkOperation() )

    (0 until 100).foreach { _ =>
      val mer = MarkEventRecord.createRecord: MarkEventRecord
      mer.user.set(uid)
      mer.mark.set(Random.nextInt(5) + 1)
      mer.datetime.set(DateTimeUtils.now.minusDays(3).plusHours(Random.nextInt(50)))
      op.insert(mer.asDBObject)
    }

    op.execute(WriteConcern.ACKNOWLEDGED)
  }

  "learningStats" - {
    "don't blow up" in {
      createSomeData()
      createSomeData()
      val stats = LearningStats.recentLearningMR(10)
      val tdata = LearningStats.transformMrData(stats)
      tdata should not be Nil
    }
  }
}
