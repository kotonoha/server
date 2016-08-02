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

package ws.kotonoha.server.web.rest

import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.{JsonResponse, PlainTextResponse}
import ws.kotonoha.server.records._
import concurrent.Future
import ws.kotonoha.server.actors.schedulers.RepetitionStateResolver
import net.liftweb.json.{Extraction, DefaultFormats}
import net.liftweb.common.Full
import ws.kotonoha.server.util.Stat
import ws.kotonoha.server.actors.schedulers.ScheduledCardCounts
import net.liftweb.json.JsonAST.JArray
import net.liftweb.util.Props

/**
 * @author eiennohito
 * @since 01.04.12
 */

trait StatusTrait extends KotonohaRest with OauthRestHelper {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  serve {
    case "api" :: "status" :: Nil Get req => {
      async(userId) { uid =>

        val user = UserRecord.find(uid).openOrThrowException("Non-existent user")
        val x = Future.successful(new RepetitionStateResolver(uid))

        x.map { r =>
          val bldr = new StringBuilder
          if (!Props.productionMode)
            bldr.append("Warning: This is a debug kotonoha server!\n")
          bldr.append("Hello, ").append(user.username.get).append("! ")
          bldr.append("You have ").append(r.scheduledCnt).append(" cards ready, ")
          bldr.append(r.badCount).append(" bad cards, ").append(r.newAvailable).append(" new cards available for learning. ")
          bldr.append("You have repeated ").append(r.today).append(" cards today.")
          Full(PlainTextResponse(bldr.result()))
        }
      }
    }
    case List("api",  "ofmatrix") Get req => {
      import ws.kotonoha.server.util.KBsonDSL._
      val user = UserRecord.currentId
      user map (id => {
        val mid = OFMatrixRecord.forUser(id).id.get
        val items = OFElementRecord where (_.matrix eqs mid) fetch()
        val data = items map { i => ("ef" -> i.ef) ~ ("n" -> i.n) ~ ("val" -> i.value) }
        JsonResponse(JArray(data))
      })
    }
    case "api" :: "stats" :: Nil Get req => {
      implicit val formats = DefaultFormats
      val t = new Timer

      async(userId) {
        uid =>
          val f = Future.successful(new RepetitionStateResolver(uid))
          f map {
            o =>
              val words = WordRecord where (_.user eqs uid) count()
              val cards = WordCardRecord where (_.user eqs uid) count()
              val resp = Resp(
                o.resolveState().toString,
                o.last.toList,
                o.lastStat.stat,
                words,
                cards,
                o.today,
                o.badCount,
                o.newAvailable,
                o.scheduledCnt,
                o.unavailable,
                o.learnt,
                o.next
              )
              t.print(req)
              Full(JsonResponse(Extraction.decompose(resp)))
          }
      }
    }
  }
}

case class Resp(
                 state: String, last: List[Int],
                 lastStat: Stat,
                 words: Long, cards: Long,
                 today: Long,
                 badCards: Long, newCards: Long,
                 schedCards: Long, unavailableCards: Long, learntCards: Long,
                 next: ScheduledCardCounts
                 )

class StatusApi extends StatusTrait with ReleaseAkka
