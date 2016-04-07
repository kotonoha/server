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

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.matchers.ShouldMatchers
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.mongodb.mapreduce.LearningStats
import net.liftweb.json.JsonAST.{JArray, JValue}
import net.liftweb.mongodb.JObjectParser
import net.liftweb.json.DefaultFormats
import ws.kotonoha.server.mongo.MongoAwareTest


class LearningStatsTest extends FreeSpec  with Matchers with MongoAwareTest {
  implicit def formats = DefaultFormats
  "learningStats" - {
    "don't blow up" in {
      val stats = LearningStats.recentLearningMR(10)
      val tdata = LearningStats.transformMrData(stats)
      tdata should not be Nil
    }
  }
}
