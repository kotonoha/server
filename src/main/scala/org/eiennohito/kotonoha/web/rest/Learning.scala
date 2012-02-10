package org.eiennohito.kotonoha.web.rest

import org.eiennohito.kotonoha.actors.learning.{WordsAndCards, LoadWords}
import net.liftweb.common._
import net.liftweb.util.BasicTypesHelpers.AsInt
import net.liftweb.http._
import net.liftweb.http.rest._
import com.weiglewilczek.slf4s.Logging
import akka.util.{Timeout, duration}
import org.eiennohito.kotonoha.learning.ProcessMarkEvents
import net.liftweb.json.JsonAST.JString
import org.eiennohito.kotonoha.actors.ioc.{ReleaseAkka, Akka}
import org.eiennohito.kotonoha.records.MarkEventRecord
import org.eiennohito.kotonoha.utls.{ResponseUtil, UserUtil}
import akka.dispatch.Future


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


trait LearningRest extends RestHelper with Logging with Akka {
  import duration._
  import ResponseUtil._
  import akka.pattern.ask
  lazy implicit val scheduler = akkaServ.context
  lazy implicit val timeout = Timeout(5 seconds)

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
  
  serve ( "api" / "words" prefix {
    case "scheduled" :: AsInt(max) :: Nil JsonGet req => {
      val userId = UserUtil.extractUser(req) ?~ "user is not valid" ~> 403
      if (max > 50) ForbiddenResponse("number is too big")
      else async(userId) { id =>
        val f = ask(akkaServ.wordSelector, LoadWords(id, max)).mapTo[WordsAndCards]
        f map { wc => Full(JsonResponse(deuser(jsonResponse(wc)))) }
      }
    }
  })
  
  serve ( "api" / "events" prefix {
    case "mark" :: Nil JsonPut reqV => {
      val (json, req) = reqV
      val userId = UserUtil.extractUser(req) ?~ "user is not valid" ~> 403
      async(userId) { id =>
        val marks = json.children map (MarkEventRecord.fromJValue(_)) filterNot (_.isEmpty) map (_.openTheBox)
        val count = akkaServ.markProcessor ? (ProcessMarkEvents(marks))
        count.mapTo[Int] map {c => Full(JsonResponse(JString(c.toString))) }
      }      
    }
  })
}

object Learning extends LearningRest with ReleaseAkka
