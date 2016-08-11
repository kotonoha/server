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

import java.util.concurrent.atomic.AtomicReference

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.mongodb.RMData
import ws.kotonoha.server.records.UserTagInfo
import ws.kotonoha.server.util.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/11
  */
@Singleton
class WordTagOps @Inject() (
  ucx: UserContext,
  rm: RMData
)(implicit ec: ExecutionContext) extends AtomicReference[Future[TagCache]] with StrictLogging {
  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def invalidate(): Unit = set(null)
  def uid = ucx.uid

  def calculator: Future[TagCache] = {
    get() match {
      case null => synchronized {
        get() match {
          case null =>
            val x = calculate()
            set(x)
            x
          case x => x
        }
      }
      case x => x
    }
  }

  private def calculate(): Future[TagCache] = {
    val nfos = UserTagInfo.where(_.user eqs uid).limit(2000) //get 2k tags
    rm.fetch(nfos).map { lst =>
      val cached = lst.view.map(i => CachedTag(i.tag.get, i.priority.get, i.limit.get, i.usage.get))
      val cache = cached.map(i => i.tag -> i).toMap
      logger.debug(s"fetched tag information for u=$uid from db: ${cache.size} in xxx ms")
      new TagCache(cache)
    }
  }
}

case class CachedTag(tag: String, prio: Int, limit: Option[Int], usage: Long)

class TagCache(val tagCost: Map[String, CachedTag]) {

  val updateStamp = DateTimeUtils.now

  def priority(wordTags: Seq[String]): Int = {
    if (wordTags.isEmpty) return 0

    var cost = 0.0
    var len = 0

    for (t <- wordTags) {
      len += 1
      cost += tagCost.get(t).map(_.prio).getOrElse(0)
    }

    math.round(cost + 0.5).toInt
  }
}
