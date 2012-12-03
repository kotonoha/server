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

package ws.kotonoha.server.web.rest.admin

import ws.kotonoha.server.web.rest.KotonohaRest
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import net.liftweb.http.{BadResponse, JsonResponse, ForbiddenResponse, Req}
import ws.kotonoha.server.records.{MarkEventRecord, OFArchiveRecord, UserRecord}
import org.joda.time.DateTime
import net.liftweb.common.{Empty, Full}
import ws.kotonoha.server.util.unapply.XOid
import ws.kotonoha.server.supermemo.MatrixDiffCalculator
import net.liftweb.json.{DefaultFormats, Extraction}

/**
 * @author eiennohito
 * @since 01.12.12 
 */

object OFHistory extends KotonohaRest with ReleaseAkka {
  import com.foursquare.rogue.Rogue._
  import ws.kotonoha.server.util.KBsonDSL._
  import ws.kotonoha.server.util.DateTimeUtils._

  //val dateRenderer = DateTimeFormatter.

  serve(("api" / "admin" / "ofhistory") prefix {
    case Nil JsonGet r => {
      val from = new DateTime(2012, 1, 1, 0, 0)
      val to = DateTime.now()

      val ents = OFArchiveRecord where (_.timestamp between(from, to)) select(_.user, _.id, _.timestamp)
      val res = ents.orderAsc(_.user).andAsc(_.timestamp).fetch()

      val data = res.map {
        case (u, id, ts) => {
          val dt: DateTime = ts
          ("user" -> u) ~ ("time" -> ts.toDate) ~ ("id" -> id)
        }
      }
      Full(JsonResponse(data))
    }
    case "compare" :: Nil JsonGet req => {
      val l = req.param("l")
      val r = req.param("r")
      val its = (l, r) match {
        case (Full(XOid(li)), Full(XOid(ri))) => {
          val le = OFArchiveRecord where (_.id eqs li) get()
          val re = OFArchiveRecord where (_.id eqs ri) get()
          Full((le, re))
        }
        case _ => Empty
      }
      its match {
        case Full((Some(l), Some(r))) => {
          if (l.user.is != r.user.is) {
            Full(BadResponse())
          } else if (l.timestamp.is.after(r.timestamp.is)) {
            Full(BadResponse())
          } else {
            val model = MatrixDiffCalculator.model(l, r)
            val json = Extraction.decompose(model)(DefaultFormats)
            Full(JsonResponse(json))
          }
        }
        case _ => Full(BadResponse())
      }
    }
  })

  override def apply(in: Req) = {
    //val u = UserRecord.isAdmin
    val u = true
    if (u) {
      super.apply(in)
    } else {
      () => Some(ForbiddenResponse("You are not admin"))
    }
  }
}
