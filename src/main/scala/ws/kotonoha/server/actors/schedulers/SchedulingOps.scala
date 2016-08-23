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

package ws.kotonoha.server.actors.schedulers

import javax.inject.Inject

import akka.NotUsed
import akka.stream.scaladsl.Source
import ws.kotonoha.server.actors.learning.{LoadCards, WordsAndCards}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.ops.WordOps
import ws.kotonoha.server.records.{WordCardRecord, WordRecord}

/**
  * @author eiennohito
  * @since 2016/08/23
  */
class SchedulingOps @Inject() (
  uc: UserContext,
  wops: WordOps
) {

  import scala.concurrent.duration._

  def cardsRepeatedInFuture(cnt: Int, skip: Int = 0): Source[WordCardRecord, NotUsed] = {
    val msg = LoadCards(cnt, skip)
    val cards = uc.askUser[WordsAndCards](msg, 10.seconds)
    Source.fromFuture(cards).mapConcat(_.cards)
  }

  def wordsRepeatedInFuture(cnt: Int, skip: Int = 0): Source[WordRecord, NotUsed] = {
    val msg = LoadCards(cnt, skip)
    val cards = uc.askUser[WordsAndCards](msg, 10.seconds)
    Source.fromFuture(cards).flatMapConcat(wnc => wops.byIds(wnc.cards.map(_.id.get)))
  }

}
