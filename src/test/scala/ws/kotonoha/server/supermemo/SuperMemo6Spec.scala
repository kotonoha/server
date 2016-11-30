/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.supermemo

import org.scalatest.{FreeSpec, LoneElement, Matchers}
import ws.kotonoha.model.sm6.{ItemCoordinate, MatrixMark}

/**
  * @author eiennohito
  * @since 2016/08/30
  */
class SuperMemo6Spec extends FreeSpec with Matchers with LoneElement {
  "supermemo" - {
    "don't fail with an initial mark of 4" in {
      val sm = SuperMemo6.create(Nil)
      val mark = MatrixMark(
        coord = ItemCoordinate(
          difficulty = 2.5f,
          inertia = 1f,
          repetition = 0,
          lapse = 0,
          interval = 1.0
        ),
        mark = 4,
        actualInterval = 1.1
      )
      val res = sm.process(mark)
      res.updates shouldBe 'empty
      res.coord.repetition shouldBe 1
    }

    "don't fail with third mark of 5" in {
      val sm = SuperMemo6.create(Nil)
      val mark = MatrixMark(
        coord = ItemCoordinate(
          difficulty = 2.5f,
          inertia = 1f,
          repetition = 2,
          lapse = 1,
          interval = 2.5f
        ),
        mark = 5,
        actualInterval = 2.5f,
        history = Seq(
          ItemCoordinate(difficulty = 2.5f, repetition = 1, lapse = 1, interval = 1.0, inertia = 1.0)
        ))
      val res = sm.process(mark)
      val item = res.updates.loneElement
      item.factor should be >= 2.5
      item.repetition shouldBe 2
      res.coord.repetition shouldBe 3
      res.coord.interval should be > (2.5 * 2.5 / 2)
    }

    "don't fail with third mark of 1" in {
      val sm = SuperMemo6.create(Nil)
      val mark = MatrixMark(
        coord = ItemCoordinate(
          difficulty = 2.5,
          inertia = 1f,
          repetition = 2,
          lapse = 1,
          interval = 2.5
        ),
        mark = 1,
        actualInterval = 2.5,
        history = Seq(
          ItemCoordinate(difficulty = 2.5f, repetition = 1, lapse = 1, interval = 1.0, inertia = 1.0)
        ))
      val res = sm.process(mark)
      val item = res.updates.loneElement
      item.factor should be < 2.5
      item.repetition shouldBe 2
      res.coord.repetition shouldBe 1
      res.coord.lapse shouldBe 2
      res.coord.difficulty should be < 2.5
    }
  }
}
