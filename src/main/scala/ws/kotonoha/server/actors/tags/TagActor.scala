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

package ws.kotonoha.server.actors.tags

import javax.inject.Inject

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.mongodb.WriteConcern
import net.liftweb.util.{Props => LP}
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.actors.model.{PrioritiesApplied, ReprioritizeCards}
import ws.kotonoha.server.actors.tags.auto.PossibleTagRequest
import ws.kotonoha.server.ops.{FlashcardOps, WordOps, WordTagOps}
import ws.kotonoha.server.records.{UserSettings, UserTagInfo, WordRecord}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * @author eiennohito
 * @since 07.01.13 
 */

class TagActor @Inject() (
  to: WordTagOps,
  wo: WordOps,
  co: FlashcardOps
) extends UserScopedActor with ActorLogging {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  implicit val timeout: Timeout = 10 seconds

  lazy val svc = Await.result((services ? ServiceActor).mapTo[ActorRef], 10 seconds)

  def handleWritingStat(writ: String, tag: String, cnt: Int): Unit = {
    svc ! GlobalTagWritingStat(writ, tag, cnt)
  }

  def handleUsage(tag: String, count: Int): Unit = {
    svc ! GlobalUsage(tag, count)
    UserTagInfo.where(_.user eqs uid).and(_.tag eqs tag).findAndModify(_.usage inc count) upsertOne (false)
  }

  def handleTagWrit(rawTag: String, writings: List[String], cnt: Int) = {
    val tag = Tags.aliases(rawTag)
    writings.foreach {
      w => handleWritingStat(w, tag, cnt)
    }
    handleUsage(tag, cnt)
  }

  def tagWord(rec: WordRecord, ops: Seq[TagOp]) {
    val wrs = rec.writing.get
    var cur = new ListBuffer() ++ rec.tags.get
    ops.foreach {
      case AddTag(tag) => {
        cur += tag
        handleTagWrit(tag, wrs, 1)
      }
      case RemoveTag(tag) if cur.contains(tag) => {
        val cnt = cur.count(_ == tag)
        cur = cur.filterNot(_ == tag)
        handleTagWrit(tag, wrs, -cnt)
      }
      case RenameTag(from, to) if cur.contains(from) => {
        val cnt = cur.count(_ == from)
        cur = cur.filterNot(_ == from) += to
        handleTagWrit(from, wrs, -cnt)
        handleTagWrit(to, wrs, 1)
      }
      case x =>
        log.debug("ignoring tagging operation {}", x)
    }
    val res = cur.result()
    val wid = rec.id.get
    val setOp = wo.setTags(wid, res)
    val prioF = to.calculator.map(_.priority(res))
    prioF.flatMap(prio => co.tagCards(wid, res, prio)) pipeTo sender()
  }

  var canPublishPrio = true

  def changeTagPriority(tag: String, pr: Int, lim: Option[Int]): Unit = {
    val q = UserTagInfo where (_.user eqs uid) and (_.tag eqs tag) modify (_.priority setTo pr)
    q modify (_.limit setTo (lim)) upsertOne (WriteConcern.SAFE)
    UserSettings where (_.id eqs uid) modify (_.stalePriorities setTo (true)) updateOne (WriteConcern.NORMAL)
    log.debug("updating tag {}: priority -> {}, limit -> {}", tag, pr, lim)
    if (canPublishPrio) {
      val time = if (LP.devMode) 5 seconds else 5 minutes;
      context.system.scheduler.scheduleOnce(time) {
        userActor ! ReprioritizeCards
      }
      canPublishPrio = false
    }
    to.invalidate()
  }

  def taglist(): Taglist = {
    val tags = UserTagInfo where (_.user eqs uid) orderDesc (_.usage) fetch()
    Taglist(tags)
  }

  override def receive = {
    case PrioritiesApplied => canPublishPrio = true
    case TagWord(wr, ops) => tagWord(wr, ops)
    case UpdateTagPriority(tag, pr, lim) => changeTagPriority(tag, pr, lim)
    case CalculatePriority(tags) => to.calculator.map(c => Priority(c.priority(tags))).pipeTo(sender())
    case TaglistRequest => sender ! taglist()
    case ptr@PossibleTagRequest =>
  }
}


case class Tagged(wid: ObjectId, tags: List[String])

