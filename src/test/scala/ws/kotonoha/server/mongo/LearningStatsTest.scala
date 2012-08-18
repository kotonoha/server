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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FreeSpec
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.mongodb.mapreduce.LearningStats

/**
 * @author eiennohito
 * @since 21.07.12
 */

class LearningStatsTest extends FreeSpec with ShouldMatchers {
  "learning stats" - {
    "at least work" in {
      MongoDbInit.init()
      val smt = LearningStats.recentLearning(2)
      smt.get("ok").asInstanceOf[Double] should equal (1.0)
//      val rcnt = LearningStats.recent(5) sortWith {
//        case (l, r) => {
//          val c = l.user.compareTo(r.user)
//          if (c == 0) {
//            if (l.date.compareTo(r.date) > 0) true else false
//          } else if (c > 0) true else false
//        }
//      } toArray
//      var i = 0
//      i += 1
    }
  }
}
