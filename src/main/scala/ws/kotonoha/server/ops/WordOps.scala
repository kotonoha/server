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

import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.bson.types.ObjectId
import ws.kotonoha.akane.unicode.KanaUtil
import ws.kotonoha.examples.api.ExamplePack
import ws.kotonoha.model.{CardMode, WordStatus}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.WordRecord

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/08
  */
class WordOps @Inject() (
  uc: UserContext,
  rm: RMData,
  tops: WordTagOps,
  cops: FlashcardOps
)(implicit ec: ExecutionContext) extends StrictLogging {
  import OpsExtensions._
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  private def qbyId(wid: ObjectId) = {
    WordRecord.where(_.id eqs wid).and(_.user eqs uc.uid)
  }

  def byId(id: ObjectId) = {
    rm.fetch(qbyId(id).limit(1)).map(_.headOption)
  }

  def byIds(ids: Traversable[ObjectId]): Source[WordRecord, NotUsed] = {
    val q = WordRecord.where(_.user eqs uc.uid).and(_.id in ids)
    rm.stream(q)
  }

  def exceptIds(ids: Traversable[ObjectId]): Source[WordRecord, NotUsed] = {
    val q = WordRecord.where(_.user eqs uc.uid).and(_.id nin ids)
    rm.stream(q)
  }

  def register(word: WordRecord, status: WordStatus): Future[NotUsed] = {
    val wordid = word.id.get

    val enabled = WordOps.enableCards(status)

    def calcInfo(prio: Int) = {
      val rd = if (WordOps.needsReadingCard(word)) {
        List(CreateCard(CardMode.Reading, prio, enabled))
      } else Nil
      CreateCard(CardMode.Writing, prio, enabled) :: rd
    }

    val f1 = rm.save(Seq(word.status(status)))
    for {
      prio <- tops.calculator.map(_.priority(word.tags.get))
      _ <- cops.register(wordid, calcInfo(prio))
      _ <- f1
    } yield NotUsed
  }

  def setTags(wid: ObjectId, tags: List[String]): Future[NotUsed] = {
    val q = qbyId(wid).modify(_.tags setTo tags)
    rm.update(q).mod(1)
  }

  def setRepExamples(wid: ObjectId, pack: ExamplePack): Future[Done] = {
    val upd = qbyId(wid).modify(_.repExamples.setTo(pack))
    rm.update(upd).mod(1, Done)
  }

  def updateLink(wid: ObjectId, jmd: Long): Future[NotUsed] = {
    val q = qbyId(wid).modify(_.jmdictLink.setTo(jmd))
    rm.update(q).mod(1)
  }
}

object WordOps {
  def needsReadingCard(word: WordRecord): Boolean = {
    val read = word.reading.stris
    val writ = word.writing.stris
    if (writ == null || writ == "") {
      word.writing(word.reading.get)
      false
    } else {
      //normalized katakana == hiragana equals
      !KanaUtil.kataToHira(read).equalsIgnoreCase(KanaUtil.kataToHira(writ))
    }
  }

  def enableCards(status: WordStatus) = {
    status match {
      case WordStatus.Approved => true
      case _ => false
    }
  }
}
