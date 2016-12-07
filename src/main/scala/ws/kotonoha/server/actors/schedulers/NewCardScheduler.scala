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

import akka.actor.ActorLogging
import com.foursquare.rogue.Iter
import org.bson.types.ObjectId
import ws.kotonoha.model.CardMode
import ws.kotonoha.server.actors.UserScopedActor
import ws.kotonoha.server.records.events.NewCardSchedule
import ws.kotonoha.server.records.{UserTagInfo, WordCardRecord}

import scala.annotation.tailrec
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
 * @author eiennohito
 * @since 27.02.13 
 */

class NewCardScheduler extends UserScopedActor with ActorLogging {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  case class CacheItem(cid: ObjectId, tags: List[String], mode: CardMode, word: ObjectId)

  private[schedulers] def limits() = {
    val list = UserTagInfo where (_.user eqs uid) and
      (_.limit exists true) and
      (_.limit neqs 0) select(_.tag, _.limit) fetch()
    list.map {
      case (tag, limit) => tag -> limit.get
    } toMap
  }

  private def tagUsage() = {
    val q = NewCardSchedule where (_.user eqs uid) and
      (_.date gt (now.minusDays(1))) select (_.tag)
    val usage = q.iterate(Map[String, Int]()) {
      case (m, Iter.Item(tag)) =>
        Iter.Continue(m.updated(tag, m.getOrElse(tag, 0) + 1))
      case (m, _) => Iter.Return(m)
    }
    usage
  }

  private[schedulers] def bannedTags(lims: Map[String, Int], usg: Map[String, Int] = tagUsage()): List[String] = {
    val ks = usg filter {
      case (tag, cnt) => cnt >= lims(tag)
    }
    ks.keys.toList
  }

  private def query(cnt: Int, ignore: Vector[ObjectId], banned: List[String]) = {
    log.debug("new query: ignored words: {}, banned tags: [{}]", ignore.length, banned.mkString(", "))
    Queries.newCards(uid) and (_.word nin ignore) and (_.tags nin banned) orderDesc
      (_.priority) select(_.id, _.tags, _.cardMode, _.word) fetch (cnt)
  }

  private def processUsage(data: Vector[CacheItem]) = {
    val lims = limits()
    data foreach {
      case CacheItem(cid, tags, _, _) => tags foreach {
        tag =>
          if (lims.contains(tag)) {
            val entry = NewCardSchedule.createRecord
            entry.user(uid).card(cid).date(now).tag(tag)
            entry.save()
          }
      }
    }
  }

  private def checkState(item: CacheItem, cur: TraversableOnce[CacheItem]): Boolean = {
    def has(x: TraversableOnce[CacheItem]) = x.exists(ci => ci.word == item.word && ci.mode != item.mode)

    !(has(cur) || has(cached))
  }

  private def cutWords(vector: Vector[CacheItem]) = {
    val buf = new ListBuffer[CacheItem]()
    for (i <- vector) {
      if (checkState(i, buf)) {
        buf += i
      }
    }
    buf.toVector
  }

  private def fetchUpdate(cnt: Int, qIgnore: Seq[ObjectId]) = {
    val lims = limits().withDefaultValue(Int.MaxValue)

    @tailrec
    def rec(rem: Int, usg: Map[String, Int], banned: List[String], prev: Vector[CacheItem]): Vector[CacheItem] = {
      val ignore = (cached ++ prev).map(_.word) ++ qIgnore
      val objs = query(cnt, ignore, banned) map CacheItem.tupled
      val bldr = new VectorBuilder[CacheItem]()

      val usm = objs.foldLeft(usg) {
        case (u, i) =>
          resolveUpdateUsage(i.tags, u, lims) match {
            case Some(r) =>
              bldr += i
              r
            case None =>
              u
          }
      }
      val res = bldr.result()
      val len = res.length
      if (len == 0) {
        log.debug("iter: got 0 after filtering, was {}", objs.length)
        prev
      } else if (len > rem) {
        prev ++ res
      } else {
        rec(rem - len, usm, bannedTags(lims, usm), prev ++ res)
      }
    }

    rec(cnt, tagUsage(), bannedTags(lims), Vector.empty)
  }


  private def resolveUpdateUsage(tags: List[String], u: Map[String, Int], lims: Map[String, Int]): Option[Map[String, Int]] = {
    tags.foldLeft[Option[Map[String, Int]]](Some(u)) {
      case (u, t) =>
        if (u.isDefined) {
          val u1 = u.get
          val cur = u1.getOrElse(t, 0)
          if (lims(t) > cur) Some(u1.updated(t, cur + 1)) else None
        } else None
    }
  }

  var cached = Vector[CacheItem]()

  private def select(cnt: Int, qIgnore: Seq[ObjectId]): List[CacheItem] = {
    if (cached.length < cnt) {
      val oldlen = cached.length
      val got = Random.shuffle(fetchUpdate(cnt * 2, qIgnore))
      val dwords = cutWords(got)
      cached = (cached ++ dwords).distinct
      log.debug("updated cache: {} -> {}", oldlen, cached.length)
    }
    cached.take(cnt).toList
  }

  private def commit(cnt: Int): Unit = {
    val dropped = cached.take(cnt)
    processUsage(dropped)
    cached = cached.drop(cnt)
  }

  def preloadToday(): Unit = {
    val today = NewCardSchedule where (_.user eqs uid) and (_.date gt now.minusDays(1)) select(_.card) fetch()
    val data = WordCardRecord where (_.id in today) and (_.learning exists(false)) select(_.id, _.tags, _.cardMode, _.word) fetch()
    cached = data.map(CacheItem.tupled).toVector
  }

  override def preStart() {
    super.preStart()
    self ! LoadUnscheduled
  }


  def receive = {
    case c: CardRequest =>
      val entries = select(c.reqLength, c.ignoreWords)
      sender ! PossibleCards(entries.map {
        c => ReviewCard(c.cid, c.word, "New")
      })
    case CardsSelected(cnt) => commit(cnt)
    case LoadUnscheduled => preloadToday()
  }
}

case object LoadUnscheduled
