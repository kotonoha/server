package ws.kotonoha.server.web.rest

import ws.kotonoha.server.actors.learning.{LoadReviewList, WordsAndCards, LoadWords}
import net.liftweb.common._
import net.liftweb.util.BasicTypesHelpers.AsInt
import net.liftweb.http._
import net.liftweb.http.rest._
import com.weiglewilczek.slf4s.Logging
import akka.util.{Timeout, duration}
import ws.kotonoha.server.actors.ioc.{ReleaseAkka, Akka}
import akka.dispatch.Future
import net.liftweb.json.JsonAST.{JField, JObject, JString}
import ws.kotonoha.server.util.{DateTimeUtils, ResponseUtil, UserUtil}
import ws.kotonoha.server.records.{ChangeWordStatusEventRecord, AddWordRecord, MarkEventRecord}
import ws.kotonoha.server.learning.{ProcessWordStatusEvent, ProcessMarkEvents}


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
}

trait KotonohaRest extends RestHelper with Logging with Akka {
  import duration._
  lazy implicit val scheduler = akkaServ.context
  lazy implicit val timeout = Timeout(5 seconds)

  def async[Obj](param: Future[Obj])(f: (Obj => Future[Box[LiftResponse]])) = {
      RestContinuation.async({resp =>
        param onComplete {
          case Left(ex) => {
            logger.error("Error in getting parameter", ex)
            resp(PlainTextResponse("Internal server error", 500))
          }
          case Right(v) => {
            val fut = f(v)
            val tCancel = akkaServ.schedule(() => resp(PlainTextResponse("Sevice timeouted", 500)), 10 seconds)

            fut onSuccess {
              case Full(r) => resp(r); tCancel.cancel()
              case x @ _ => logger.debug("found something: " + x)
            }
          }
        }
      })
    }

  def async[Obj](param: Box[Obj])(f: (Obj => Future[Box[LiftResponse]])) = {
        RestContinuation.async({resp =>
          param match {
            case Empty => resp(PlainTextResponse("No response", 500))
            case b: EmptyBox => {
              emptyToResp(b) map (resp(_))
            }
            case Full(v) => {
              val fut = f(v)
              val tCancel = akkaServ.schedule(() => resp(PlainTextResponse("Sevice timeouted", 500)), 10 seconds)

              fut onSuccess {
                case Full(r) => resp(r); tCancel.cancel()
                case x @ _ => logger.debug("found something: " + x)
              }
            }
          }
        })
      }
}

trait LearningRest extends KotonohaRest with OauthRestHelper {

  import ResponseUtil._
  import akka.pattern.ask

//  def async[Obj](box: Box[Obj])(f : (Obj, ( => LiftResponse) => Unit) => Unit) = {
//     RestContinuation.async { req =>      
//       Schedule.schedule(() => req(PlainTextResponse("Sevice timeouted", 500)), ts(10 seconds))
//       
//       box match {
//         case Full(o) => f(o, req)
//         case smt : EmptyBox => emptyToResp(smt) map { req(_) }
//         case x @ _ => logger.debug("found this shit in async response: %s".format(x))
//       }
//     }
//   }

  
  serve ( "api" / "words" prefix {
    case "scheduled" :: AsInt(max) :: Nil JsonGet req => {
      val t = new Timer
      if (max > 50) ForbiddenResponse("number is too big")
      else async(userId) { id =>
        val f = ask(akkaServ.wordSelector, LoadWords(id, max)).mapTo[WordsAndCards]
        f map { wc => t.print(); Full(JsonResponse(deuser(jsonResponse(wc)))) }
      }
    }

    case "review" :: AsInt(max) :: Nil JsonGet req => {
      val t = new Timer
      if (max > 50) ForbiddenResponse("number is too big")
      else async(userId) { id =>
        val f = ask(akkaServ.wordSelector, LoadReviewList(id, max)).mapTo[WordsAndCards]
        f map { wc => t.print(); Full(JsonResponse(deuser(jsonResponse(wc)))) }
      }
    }
  })
  
  import net.liftweb.mongodb.BsonDSL._
  import ws.kotonoha.server.util.ResponseUtil.Tr
  
  serve ( "api" / "events" prefix {
    case "mark" :: Nil JsonPost reqV => {
      val t = new Timer()
      val (json, req) = reqV
      async(userId) { id =>
        val marks = json.children map (MarkEventRecord.fromJValue(_)) filterNot (_.isEmpty) map (_.openTheBox) map (_.user(id))
        logger.info("posing %d marks for user %d".format(marks.length, id))
        val count = akkaServ.eventProcessor ? (ProcessMarkEvents(marks))
        count.mapTo[List[Int]] map {c => t.print(); Full(JsonResponse("values" -> Tr(c))) }
      }      
    }

    case List("add_words") JsonPost reqV => {
      val (json, req) = reqV
      val add = json.children map (AddWordRecord.fromJValue(_)) filterNot (_.isEmpty) map (_.openTheBox) map (_.user(userId))
      JsonResponse("values" -> Tr(add.map {
        a => a.save
        1
      }))
    }

    case List("change_word_status") JsonPost reqV => {
      val (json, req) = reqV
      val chs = json.children map (ChangeWordStatusEventRecord.fromJValue(_)) filterNot (_.isEmpty) map (_.openTheBox) map (_.user(userId))
      async(userId) { id =>
        val f = (akkaServ ? ProcessWordStatusEvent(chs)).mapTo[List[Int]]
        f.map {o => Full(JsonResponse("values" -> Tr(o)))}
      }
    }
  })

  override protected def jsonResponse_?(in: Req) = true
}

object Learning extends LearningRest with ReleaseAkka
