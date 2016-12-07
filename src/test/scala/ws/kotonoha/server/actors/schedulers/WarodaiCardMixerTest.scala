/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import org.bson.types.ObjectId
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * @author eiennohito
 * @since 28.02.13 
 */

class WarodaiCardMixerTest extends FreeSpec with Matchers {
  implicit val ec = ExecutionContext.global

  import concurrent.duration._

  def oids(num: Int): CardProvider = new CardProvider {
    def request(req: CardRequest) = {
      val cards = 1 to num map {
        _ => new ObjectId()
      }
      Future.successful(cards.map(c => ReviewCard(c, c, "None")).toList)
    }

    def selected(count: Int) {}
  }

  "CardMixer" - {
    "with two seqs should give good answer" in {
      val mixer = CardMixer(CardSource(oids(5), 1.0), CardSource(oids(10), 2.0))
      val req = Requests.ready(10)
      val data = Await.result(mixer.process(req), 10 minutes)
      data.length should be > (5)
    }
  }
}
