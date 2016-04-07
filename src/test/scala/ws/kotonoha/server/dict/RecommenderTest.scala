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

package ws.kotonoha.server.dict

import org.bson.types.ObjectId
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.akane.juman.JumanPipeExecutor
import ws.kotonoha.server.actors.recommend.RecommendRequest
import ws.kotonoha.server.mongo.MongoAwareTest

/**
 * @author eiennohito
 * @since 20.03.13 
 */

class RecommenderTest extends FreeSpec with Matchers with MongoAwareTest {
  val juman = JumanPipeExecutor.apply()

  "recommender" - {
    lazy val rec = new Recommender(new ObjectId())
    "should give something for watashi" ignore {
      val wr = "私"
      val rd = "わたし"
      val jd = juman.parse(wr)
      val req = RecommendRequest(Some(wr), Some(rd), jd)
      rec.preprocess(req).length should be >= (2)
    }
  }

  "recommenders with watashi" - {
    val wr = "私"
    val rd = "わたし"
    val jum = juman.parse(wr)
    lazy val dr = DoRecommend(wr, rd, jum)

    "kun recommender should give at least watashi" ignore {
      val rec = new SingleKunRecommender(100)
      val res = rec.apply(dr)
      val processed = res.flatMap(_.select(Nil)).distinct
      processed.length should be >= (1)
    }

    "jukugo recommender should give a lot of different things" ignore {
      val rec = new SimpleJukugoRecommender(100)
      val res = rec.apply(dr)
      val processed = res.flatMap(_.select(Nil)).distinct
      processed.length should be >= (1)
    }
  }

  override protected def afterAll() = {
    super.afterAll()
    juman.close()
  }
}
