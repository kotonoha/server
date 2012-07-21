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

package org.eiennohito.kotonoha.web.rest

import net.liftweb.http.rest.RestHelper
import org.eiennohito.kotonoha.mongodb.mapreduce.LearningStats
import org.eiennohito.kotonoha.records.UserRecord
import net.liftweb.json.JsonAST.{JString, JArray, JValue}
import org.eiennohito.kotonoha.util.DateTimeUtils
import net.liftweb.http.{Req, JsonResponse}
import net.liftweb.common.Full

/**
 * @author eiennohito
 * @since 21.07.12
 */

object Stats extends RestHelper {
  import org.eiennohito.kotonoha.util.KBsonDSL._

  serve("ajax" / "stats" prefix {
    case List("learning") JsonGet req => {
      val stats = LearningStats.recent(10)
      val last = stats groupBy(_.user)
      val users = UserRecord findAllByList(last.keys.toList) map {u => u.id.is -> u} toMap
      val days = DateTimeUtils.last10midn
      val jvals : Iterable[JValue] = last map {
        case (uid, data) => {
          val fromDate = data.map {d => d.date -> d}.toMap
          val uname = users(uid).shortName
          JArray(JString(uname) :: (days map {
            d => fromDate.get(d) map {
              s => "%d (%.3f)".format(s.total, s.avgMark)} getOrElse("")
          } map JString ))
        }
      }
      val ucnt = users.size
      val resp = ("sEcho" -> 1) ~ ("iTotalRecords" -> ucnt) ~ ("iTotalDisplayRecords" -> ucnt) ~
      ("aaData" -> JArray(jvals.toList))
      JsonResponse(resp)
    }
  })

  override protected def jsonResponse_?(in: Req) = true
}
