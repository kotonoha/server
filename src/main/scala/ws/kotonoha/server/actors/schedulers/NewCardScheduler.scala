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

import ws.kotonoha.server.actors.UserScopedActor
import collection.mutable.ListBuffer
import org.bson.types.ObjectId
import ws.kotonoha.server.records.events.NewCardSchedule
import com.foursquare.rogue.Iter
import ws.kotonoha.server.records.UserTagInfo
import util.Random
import collection.immutable.VectorBuilder
import akka.actor.ActorLogging

/**
 * @author eiennohito
 * @since 27.02.13 
 */

class NewCardScheduler extends UserScopedActor with ActorLogging {

  import com.foursquare.rogue.LiftRogue._
  import ws.kotonoha.server.util.DateTimeUtils._

  def bannedTags: List[String] = {
    //NewCardSchedule where (_.user eqs uid) and (_.date lt now.minusDays(1)) bulkDelete_!!(WriteConcern.Safe)
    val q = NewCardSchedule where (_.user eqs uid) and
      (_.date gt (now.minusDays(1))) select (_.tag)
    val usage = q.iterate(Map[String, Int]()) {
      case (m, Iter.Item(tag)) =>
        Iter.Continue(m.updated(tag, m.get(tag).getOrElse(0) + 1))
      case (m, _) => Iter.Return(m)
    }

    val tags = {
      val list = UserTagInfo where (_.user eqs uid) and (_.limit exists true) select(_.tag, _.limit) fetch()
      list.map {
        case (tag, limit) => tag -> limit.get
      } toMap
    }

    val ks = usage filter {
      case (tag, cnt) => cnt > tags(tag)
    }
    ks.keys.toList
  }

  def query(cnt: Int, ignore: Vector[ObjectId]) = {
    Queries.newCards(uid) and (_.id nin ignore) and (_.tags nin bannedTags) orderDesc
      (_.priority) select(_.id, _.tags) fetch (cnt)
  }

  def processUsage(data: List[(ObjectId, List[String])]): List[ObjectId] = {
    val entries = data flatMap {
      case (cid, tags) => tags map {
        tag =>
          val entry = NewCardSchedule.createRecord
          entry.user(uid).card(cid).date(now).tag(tag)
      }
    }
    NewCardSchedule.insertAll(entries)
    data.map(_._1)
  }

  def fetchUpdate(cnt: Int) = {
    val bldr = new VectorBuilder[ObjectId]()

    var rem = cnt
    while (rem > 0) {
      val objs = query(cnt, cached ++ bldr.result())
      val processed = processUsage(objs)
      bldr ++= processed
      rem -= processed.length
    }

    bldr.result()
  }

  var cached = Vector[ObjectId]()

  private def select(cnt: Int): List[ObjectId] = {
    if (cached.length < cnt) {
      val oldlen = cached.length
      cached = Random.shuffle((cached ++ fetchUpdate(cnt * 2)).distinct)
      log.debug("updated cache: {} -> {}", oldlen, cached.length)
    }
    cached.take(cnt).toList
  }

  private def commit(cnt: Int): Unit = {
    cached = cached.drop(cnt)
  }

  def receive = {
    case CardRequest(_, _, _, _, cnt) =>
      val entries = select(cnt)
      sender ! PossibleCards(entries.map {
        cid => ReviewCard(cid, "New")
      })
    case CardsSelected(cnt) => commit(cnt)
  }
}
