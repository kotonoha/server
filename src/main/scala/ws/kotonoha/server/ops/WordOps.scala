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

package ws.kotonoha.server.ops

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.bson.types.ObjectId
import ws.kotonoha.model.{CardMode, WordStatus}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.{WordCardRecord, WordRecord}
import ws.kotonoha.server.util.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/08
  */
class WordOps @Inject() (uc: UserContext) (
  implicit ec: ExecutionContext
) extends StrictLogging {
  def register(rec: WordRecord, status: WordStatus) = ???
}

case class CreateCard(mode: CardMode, priority: Int, cid: ObjectId = ObjectId.get())

class FlashcardOps @Inject() (
  uc: UserContext,
  rm: RMData
)(implicit ec: ExecutionContext) extends StrictLogging {
  def register(wid: ObjectId, mode: CardMode, priority: Int): Future[ObjectId] = {
    this.register(wid, Seq(CreateCard(mode, priority))).map(_.head)
  }

  def register(wid: ObjectId, info: Seq[CreateCard]): Future[Seq[ObjectId]] = {
    val nbef = DateTimeUtils.now.minusMinutes(5)
    val cards = info.map { cc =>
      val card = WordCardRecord.createRecord
      card.id(cc.cid).user(uc.uid).word(wid)
        .cardMode(cc.mode.value).notBefore(nbef).priority(cc.priority)
    }

    rm.save(cards).map(_ => info.map(_.cid))
  }
}
