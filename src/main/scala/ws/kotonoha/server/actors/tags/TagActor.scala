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

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.pattern.{ask, pipe}
import auto.PossibleTagRequest
import ws.kotonoha.server.actors.UserScopedActor
import scala.concurrent.Await
import akka.util.Timeout
import concurrent.duration._
import ws.kotonoha.server.records.{UserSettings, WordTagInfo, UserTagInfo, WordRecord}
import collection.mutable.ListBuffer
import org.bson.types.ObjectId
import com.mongodb.WriteConcern
import ws.kotonoha.server.actors.model.{TagCards, PrioritiesApplied, ReprioritizeCards}
import net.liftweb.util.{Props => LP}

/**
 * @author eiennohito
 * @since 07.01.13 
 */

class TagActor extends UserScopedActor with ActorLogging {

  import com.foursquare.rogue.LiftRogue._

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

  def tagWord(rec: WordRecord, ops: List[TagOp]) {
    val wrs = rec.writing.is
    var cur = new ListBuffer() ++ rec.tags.is
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
        cur = (cur.filterNot(_ == from) += to)
        handleTagWrit(from, wrs, -cnt)
        handleTagWrit(to, wrs, 1)
      }
      case x =>
        log.debug("ignoring tagging operation {}", x)
    }
    val res = cur.result()
    WordRecord where (_.id eqs rec.id.is) modify (_.tags setTo res) updateOne()
    val prio = priority(res)
    userActor ! TagCards(rec.id.is, res, prio)
    sender ! Tagged(rec.id.is, res)
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
    priorities(tag) = pr
  }

  lazy val priorities = new PriorityCache(uid)

  def calculatePriority(tags: List[String]): Unit = {
    sender ! Priority(priority(tags))
  }

  def priority(tags: List[String]): Int = {
    val res = tags match {
      case Nil => 0
      case tags =>
        val prio = tags.foldLeft(0) {
          _ + priorities(_)
        }
        prio / tags.length
    }
    res
  }

  def taglist(): Taglist = {
    val tags = UserTagInfo where (_.user eqs uid) orderDesc (_.usage) fetch()
    Taglist(tags)
  }

  override def receive = {
    case PrioritiesApplied => canPublishPrio = true
    case TagWord(wr, ops) => tagWord(wr, ops)
    case UpdateTagPriority(tag, pr, lim) => changeTagPriority(tag, pr, lim)
    case CalculatePriority(tags) => calculatePriority(tags)
    case TaglistRequest => sender ! taglist()
    case ptr@PossibleTagRequest =>
  }
}

class PriorityCache (uid: ObjectId) {
  import com.foursquare.rogue.LiftRogue._

  def apply(tag: String): Int = cache(tag)
  def update(tag: String, prio: Int): Unit = mycache = cache.updated(tag, prio)

  private var mycache: Map[String, Int] = _

  def load(): Map[String, Int] = {
    val nfos = UserTagInfo where (_.user eqs uid) and (_.priority neqs 0) fetch()
    nfos.map(i => i.tag.is -> i.priority.is).toMap.withDefaultValue(0)
  }

  def cache = {
    if (mycache == null)
      mycache = load()
    mycache
  }
}

case class Tagged(wid: ObjectId, tags: List[String])

trait Taggable {
  def curTags: List[String]

  def writeTags(tags: List[String])
}


