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

package ws.kotonoha.server.web.rest

import ws.kotonoha.server.actors.learning.{LoadReviewList, WordsAndCards, LoadWords}
import net.liftweb.common._
import net.liftweb.util.BasicTypesHelpers.AsInt
import net.liftweb.http._
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import ws.kotonoha.server.util.ResponseUtil
import ws.kotonoha.server.learning.{ProcessWordStatusEvent, ProcessMarkEvents}
import com.typesafe.scalalogging.{StrictLogging => Logging}
import ws.kotonoha.server.records.events.{ChangeWordStatusEventRecord, AddWordRecord, MarkEventRecord}


/**
 * @author eiennohito
 * @since 04.02.12
 */


class Timer extends Logging {
  val init = System.nanoTime()

  def print() {
    val epl = System.nanoTime() - init
    val milli = epl / 1e6
    logger.debug("Current timer: timed for %.3f".format(milli))
  }

  def print(req: Req) {
    val epl = System.nanoTime() - init
    val milli = epl / 1e6
    logger.debug(f"Request to: ${req.path.wholePath.mkString("/")} <- timed $milli%.3f msec")
  }
}

trait LearningRest extends KotonohaRest {

  import ResponseUtil._
  import net.liftweb.util.ControlHelpers._

  serve("api" / "words" prefix {
    case "scheduled" :: AsInt(max) :: Nil JsonGet req => {
      val t = new Timer
      val skip = req.param("skip").flatMap(p => tryo {
        p.toInt
      }).openOr(0)
      if (skip < 0) ForbiddenResponse("skip count should be positive")
      else if (skip > 50) ForbiddenResponse("skip count is too big")
      else if (max > 50) ForbiddenResponse("number is too big")
      else async(userId) {
        id =>
          val f = userAsk[WordsAndCards](id, LoadWords(max, skip))
          f map {
            wc => t.print(req); Full(JsonResponse(jsonResponse(wc)))
          }
      }
    }

    case "review" :: AsInt(max) :: Nil JsonGet req => {
      val t = new Timer
      if (max > 50) ForbiddenResponse("number is too big")
      else async(userId) {
        id =>
          val f = userAsk[WordsAndCards](id, LoadReviewList(max))
          f map {
            wc => t.print(req); Full(JsonResponse(jsonResponse(wc)))
          }
      }
    }
  })

  import net.liftweb.mongodb.BsonDSL._
  import ws.kotonoha.server.util.ResponseUtil.Tr

  serve("api" / "events" prefix {
    case "mark" :: Nil JsonPost reqV => {
      val t = new Timer()
      val (json, req) = reqV
      val client = clientName
      async(userId) {
        id =>
          val marks = json.children flatMap (MarkEventRecord.fromJValue(_)) map (_.client(client).user(id))
          logger.info("posing %d marks for user %s".format(marks.length, id))
          val count = userAsk[List[Int]](id, ProcessMarkEvents(marks))
          count map {
            c =>
              t.print(req)
              Full(JsonResponse("values" -> Tr(c)))
          }
      }
    }

    case List("add_words") JsonPost reqV => {
      val (json, req) = reqV
      val add = json.children flatMap (AddWordRecord.fromJValue(_)) map (_.user(userId))
      JsonResponse("values" -> Tr(add.map {
        a => a.save()
        1
      }))
    }

    case List("change_word_status") JsonPost reqV => {
      val (json, req) = reqV
      val chs = json.children flatMap (ChangeWordStatusEventRecord.fromJValue(_)) map (_.user(userId))
      async(userId) {
        id =>
          val f = userAsk[List[Int]](id, ProcessWordStatusEvent(chs))
          f.map {
            o => Full(JsonResponse("values" -> Tr(o)))
          }
      }
    }
  })

  override protected def jsonResponse_?(in: Req) = true
}

object Learning extends LearningRest with ReleaseAkka
