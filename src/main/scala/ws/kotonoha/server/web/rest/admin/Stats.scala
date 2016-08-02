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

import net.liftweb.http.rest.RestHelper
import ws.kotonoha.server.mongodb.mapreduce.LearningStats
import ws.kotonoha.server.records.UserRecord
import net.liftweb.json.JsonAST.{JString, JArray, JValue}
import ws.kotonoha.server.util.DateTimeUtils
import net.liftweb.http.{Req, JsonResponse}
import net.liftweb.common.Full
import net.liftweb.mongodb.JObjectParser
import ws.kotonoha.server.mongodb.mapreduce.LearningStats.UserMarks
import org.bson.types.ObjectId
import org.joda.time.DateMidnight

/**
 * @author eiennohito
 * @since 21.07.12
 */

object Stats extends RestHelper {
  import ws.kotonoha.server.util.KBsonDSL._

  def createResponse(in: List[UserMarks], users: Map[ObjectId, UserRecord], cnt: Int) = {
    val seq = (cnt until (0, -1)).toList
    in.map(x => {
      val name = users.get(x.user).map(u => u.niceName).getOrElse(x.user.toString)
      val strs = seq map (x.reps.get(_)) map (x => "%d %d")
    })
  }

  serve("ajax" / "stats" prefix {
    case List("learning") JsonGet req => {
      val stats = LearningStats.recent(10)
      val last = stats groupBy(_.user)
      val users = UserRecord findAllByList(last.keys.toList) map {u => u.id.get -> u} toMap
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

//    case "learningMr" :: Nil JsonGet req => {
//      val stats = LearningStats.recentLearningMR(10)
//      val tfed = LearningStats.transformMrData(stats)
//      val uids = tfed.map(_.user)
//      val users = UserRecord.findAllByList(uids).map(u => u.id.is -> u).toMap
//
//    }
  })

  override protected def jsonResponse_?(in: Req) = true
}
