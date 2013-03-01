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

import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import concurrent.{Await, Future, ExecutionContext}
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 28.02.13 
 */

class CardMixerTest extends FreeSpec with ShouldMatchers {
  implicit val ec = ExecutionContext.global

  import concurrent.duration._

  def oids(num: Int): CardProvider = new CardProvider {
    def request(req: CardRequest) = {
      val cards = 1 to num map {
        _ => new ObjectId()
      }
      Future.successful(cards.map(c => ReviewCard(c, "None")).toList)
    }

    def selected(count: Int) {}
  }

  "CardMixer" - {
    "with two seqs should give good answer" in {
      val mixer = CardMixer(Source(oids(5), 1.0), Source(oids(10), 2.0))
      val req = CardRequest(State.Normal, 0, 0, 0, 20)
      val data = Await.result(mixer.process(req), 10 minutes)
      data.length should be > (5)
    }
  }
}
