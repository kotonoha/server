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

package ws.kotonoha.server.util

import com.typesafe.scalalogging.StrictLogging
import net.liftweb.http.js.JsCmd
import net.liftweb.http.rest.RestContinuation
import net.liftweb.http.{JavaScriptResponse, OkResponse}

import scala.concurrent.{ExecutionContext, Future}

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
 * @since 06.03.12
 */

object ParseUtil {
  def hexLong(s: String): Long = {
    val number = BigInt(s, 16)
    number.longValue()
  }
}

object Snippets extends StrictLogging {

  object XString {
    def unapply(in: Any): Option[String] = in match {
      case s: String => Some(s)
      case _ => None
    }
  }

  object XArrayNum {
    def unapply(in: Any): Option[List[Number]] =
      in match {
        case lst: List[_] => Some(lst.flatMap {
          case n: Number => List(n)
          case _ => Nil
        })
        case _ => None
      }
  }


  def async(f: Future[JsCmd])(implicit ec: ExecutionContext, loc: sourcecode.FullName): JsCmd = RestContinuation.async((resp) => {
    f.onComplete {
      case scala.util.Success(s) => resp.apply(new JavaScriptResponse(s, Nil, Nil, 200))
      case scala.util.Failure(e) =>
        logger.error(s"${loc.value} async call failed: ", e)
        resp.apply(OkResponse())
    }
  })
}
