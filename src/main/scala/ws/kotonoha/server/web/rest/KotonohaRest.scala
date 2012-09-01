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

import net.liftweb.http.rest.{RestContinuation, RestHelper}
import com.weiglewilczek.slf4s.Logging
import ws.kotonoha.server.actors.ioc.Akka
import akka.util.{Timeout, duration}
import akka.dispatch.{KeptPromise, Future}
import net.liftweb.common._
import net.liftweb.http.{JsonResponse, PlainTextResponse, LiftResponse}
import net.liftweb.common.Full

trait KotonohaRest extends RestHelper with Logging with Akka {

  import duration._

  lazy implicit val scheduler = akkaServ.context
  lazy implicit val timeout = Timeout(5 seconds)

  def async[Obj](param: Future[Obj])(f: (Obj => Future[Box[LiftResponse]])) = {
    RestContinuation.async({
      case resp =>
        param onComplete {
          case Left(ex) => {
            logger.error("Error in getting parameter", ex)
            resp(PlainTextResponse("Internal server error", 500))
          }
          case Right(v) => {
            val fut = f(v)
            val tCancel = akkaServ.schedule(() => resp(PlainTextResponse("Sevice timeouted", 500)), 10 seconds)

            fut onSuccess {
              case Full(r) => tCancel.cancel(); resp(r)
              case x@_ => logger.debug("found something: " + x)
            }
          }
        }
    })
  }

  def async[Obj](param: Box[Obj])(f: (Obj => Future[Box[LiftResponse]])) = {
    RestContinuation.async({
      case resp =>
        param match {
          case Empty => resp(PlainTextResponse("No response", 500))
          case b: EmptyBox => {
            emptyToResp(b) map (resp(_))
          }
          case Full(v) => {
            val fut = f(v)
            val tCancel = akkaServ.schedule(() => resp(PlainTextResponse("Sevice timeouted", 500)), 10 seconds)

            fut onSuccess {
              case Full(r) => tCancel.cancel(); resp(r)
              case x@_ => logger.debug("found something: " + x)
            }
          }
        }
    })
  }

  def kept[T <% LiftResponse](obj: T) = {
    new KeptPromise[Box[LiftResponse]](Right(Full(obj)))(akkaServ.system.dispatcher)
  }
}
