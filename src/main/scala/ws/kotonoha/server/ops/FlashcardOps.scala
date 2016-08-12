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

import java.util.concurrent.TimeUnit

import akka.NotUsed
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.bson.types.ObjectId
import org.joda.time.DateTime
import ws.kotonoha.model.{CardMode, WordStatus}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.math.MathUtil
import ws.kotonoha.server.mongodb.{KotonohaLiftRogue, RMData}
import ws.kotonoha.server.records.WordCardRecord
import ws.kotonoha.server.util.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/11
  */
case class CreateCard(mode: CardMode, priority: Int, enabled: Boolean, cid: ObjectId = ObjectId.get())

class FlashcardOps @Inject() (
  uc: UserContext,
  rm: RMData
)(implicit ec: ExecutionContext) extends StrictLogging {

  import DateTimeUtils._
  import KotonohaLiftRogue._
  import OpsExtensions._

  import scala.concurrent.duration._

  private def uid = uc.uid

  private def forWord(wid: ObjectId) = {
    WordCardRecord.where(_.word.eqs(wid)).and(_.user.eqs(uid))
  }

  def register(wid: ObjectId, mode: CardMode, priority: Int, enabled: Boolean): Future[ObjectId] = {
    this.register(wid, Seq(CreateCard(mode, priority, enabled))).map(_.head)
  }

  def register(wid: ObjectId, info: Seq[CreateCard]): Future[Seq[ObjectId]] = {
    val nbef = DateTimeUtils.now.minusMinutes(5)
    val cards = info.map { cc =>
      val card = WordCardRecord.createRecord
      card.id(cc.cid).user(uid).word(wid).enabled(cc.enabled)
        .cardMode(cc.mode).notBefore(nbef).priority(cc.priority)
    }

    rm.save(cards).mod(info.size, info.map(_.cid))
  }

  def otherCardsAfter(wid: ObjectId, myMode: CardMode, nextDate: DateTime): Future[NotUsed] = {
    val query = forWord(wid).and(_.cardMode.neqs(myMode)).modify(_.notBefore setTo nextDate)
    rm.update(query, multiple = true).minMod(1)
  }

  def schedulePaired(wid: ObjectId, ignoreMode: CardMode): Future[NotUsed] = {
    val nextDate = now.plus((3.2 * MathUtil.ofrandom).days)
    otherCardsAfter(wid, ignoreMode, nextDate)
  }

  def scheduleAfter(cid: ObjectId, interval: FiniteDuration): Future[NotUsed] = {
    val nextDate = now.plus(interval)
    val query = forId(cid).modify(_.notBefore.setTo(nextDate))
    rm.update(query).mod(1)
  }

  private def forId(cid: ObjectId) = {
    WordCardRecord.where(_.id.eqs(cid)).and(_.user.eqs(uid))
  }

  def clearNotBefore(cid: ObjectId): Future[NotUsed] = {
    logger.debug("Clearning not before for card id {}", cid)
    scheduleAfter(cid, FiniteDuration(-5, TimeUnit.SECONDS))
  }

  def enableFor(wid: ObjectId, to: Boolean) = {
    val q = forWord(wid).modify(_.enabled.setTo(to))
    rm.update(q, multiple = true).minMod(1)
  }

  def tagCards(wid: ObjectId, tags: List[String], prio: Int): Future[NotUsed] = {
    val q = forWord(wid).modify(_.tags.setTo(tags)).and(_.priority.setTo(prio))
    rm.update(q, multiple = true).minMod(1)
  }

  def deleteFor(wid: ObjectId): Future[NotUsed] = {
    val q = forWord(wid)
    rm.remove(q).minMod(1)
  }

  def setPriority(cid: ObjectId, prio: Int): Future[NotUsed] = {
    val q = forId(cid).modify(_.priority.setTo(prio))
    rm.update(q).mod(1)
  }
}