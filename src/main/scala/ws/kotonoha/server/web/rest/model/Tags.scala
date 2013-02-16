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

package ws.kotonoha.server.web.rest.model

import ws.kotonoha.server.web.rest.KotonohaRest
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.{S, InternalServerErrorResponse, LiftResponse, JsonResponse}
import net.liftweb.json.JsonAST.JValue
import concurrent.{ExecutionContext, Future}
import net.liftweb.http.rest.RestContinuation
import util.{Failure, Success}
import net.liftweb.common.{Box, Loggable}

/**
 * @author eiennohito
 * @since 16.02.13 
 */

object Converter extends Loggable {
  implicit def scalaconcfuture2LiftResponse[T <% LiftResponse](fut: Future[T])(implicit ec: ExecutionContext): () => Box[LiftResponse] = {
    RestContinuation.async {
      finish =>
        fut.onComplete {
          case Success(res) => finish(res)
          case Failure(ex) =>
            logger.error("error when completing request", ex)
            finish(InternalServerErrorResponse())
        }
    }
  }
}

trait TagsT extends KotonohaRest {

  import ws.kotonoha.server.util.KBsonDSL._
  import Converter._

  serve("api" / "model" / "tags" prefix {
    case Nil JsonGet req =>
      val jv: JValue = "nothing"
      Future.successful(jv)
  })
}

object Tags extends KotonohaRest with ReleaseAkka
