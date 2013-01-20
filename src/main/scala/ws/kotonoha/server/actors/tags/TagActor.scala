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

import akka.actor.{Actor, ActorRef}
import akka.pattern.{ask, pipe}
import auto.PossibleTagRequest
import ws.kotonoha.server.actors.UserScopedActor
import scala.concurrent.Await
import akka.util.Timeout
import concurrent.duration._
import ws.kotonoha.server.records.{WordTagInfo, UserTagInfo, WordRecord}
import collection.mutable.ListBuffer
import org.bson.types.ObjectId
import org.specs2.specification.TagsFragments.TaggedAs

/**
 * @author eiennohito
 * @since 07.01.13 
 */

class TagActor extends UserScopedActor {

  import com.foursquare.rogue.LiftRogue._

  implicit val timeout: Timeout = 10 seconds

  lazy val svc = Await.result((services ? ServiceActor).mapTo[ActorRef], 10 seconds)

  def handleWritingStat(writ: String, tag: String, cnt: Int): Unit = {
    svc ! GlobalTagWritingStat(writ, tag, cnt)
    Tags.handleWritingStat(writ, tag, cnt, uid)
  }

  def handleUsage(tag: String, count: Int): Unit = {
    svc ! GlobalUsage(tag, count)
    val res = UserTagInfo.where(u => objectIdFieldToObjectIdQueryField(u.user).eqs(uid)).and(_.tag eqs tag).findAndModify(_.usage inc count) updateOne (false)
    res match {
      case None if count > 0 => UserTagInfo.createRecord.user(uid).tag(tag).usage(count).save
      case _ =>
    }
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
    }
    val res = cur.result()
    WordRecord where (_.id eqs rec.id.is) modify (_.tags setTo res) updateOne()
    sender ! Tagged(rec.id.is, res)
  }

  override def receive = {
    case TagWord(wr, ops) => tagWord(wr, ops)
    case ptr@PossibleTagRequest =>
  }
}

case class Tagged(wid: ObjectId, tags: List[String])

trait Taggable {
  def curTags: List[String]

  def writeTags(tags: List[String])
}


