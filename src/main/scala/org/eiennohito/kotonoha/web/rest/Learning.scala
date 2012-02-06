package org.eiennohito.kotonoha.web.rest

import org.eiennohito.kotonoha.actors.Akka
import org.eiennohito.kotonoha.actors.learning.{WordsAndCards, LoadWords}
import org.eiennohito.kotonoha.utls.{ResponseUtil, UserUtil, DateTimeUtils}
import net.liftweb.common._
import net.liftweb.util.BasicTypesHelpers.AsInt
import net.liftweb.http._
import net.liftweb.http.rest._
import com.weiglewilczek.slf4s.Logging
import net.liftweb.util.{TimeHelpers, Schedule}
import net.liftweb.util.TimeHelpers.TimeSpan
import akka.util.{Timeout, Helpers, duration}


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


object Learning extends RestHelper with Logging {
  import duration._
  implicit val scheduler = Akka.context
  implicit val timeout = Timeout(500 milli)

  def async[Obj](box: Box[Obj])(f : (Obj, ( => LiftResponse) => Unit) => Unit) = {
     RestContinuation.async { req =>       
       Akka.schedule(() => req(PlainTextResponse("Sevice timeouted", 500)), 1 second)
       
       box match {
         case Full(o) => f(o, req)
         case smt : EmptyBox => emptyToResp(smt) map { req(_) }
         case x @ _ => logger.debug("found this shit in async response: %s".format(x))
       }
     }
   }
  
  serve ( "api" / "words" prefix {
    case "scheduled" :: AsInt(max) :: Nil JsonGet req => {
      val userId = UserUtil.extractUser(req) ?~ "user is not valid" ~> 403
      if (max > 50) ForbiddenResponse("number is too big")
      else async(userId) { (id, req) =>
        val f = akka.pattern.ask(Akka.wordSelector, LoadWords(id, max)).mapTo[WordsAndCards]
        f foreach { wc => req (ResponseUtil.jsonResponse(wc)) }
      }
    }
  })

}
