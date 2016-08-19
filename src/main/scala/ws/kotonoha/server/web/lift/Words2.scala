/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.web.lift

import com.google.inject.Inject
import net.liftweb.http.{JsonResponse, LiftResponse, NotFoundResponse}
import net.liftweb.json.JsonAST.{JField, JInt, JString}
import org.bson.types.ObjectId
import ws.kotonoha.akane.dic.jmdict.JMDictUtil
import ws.kotonoha.lift.json.JLift
import ws.kotonoha.model.WordStatus
import ws.kotonoha.server.ops.{WordExampleOps, WordJmdictOps, WordOps}
import ws.kotonoha.server.records.WordRecord
import ws.kotonoha.server.util.LangUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/18
  */
class Words2 @Inject() (
  wops: WordOps,
  wjo: WordJmdictOps,
  weo: WordExampleOps
)(implicit ec: ExecutionContext) {

  def cleanWord(w: WordRecord) = {
    val jv = w.stripped
    jv.mapField {
      case JField("status", JInt(v)) => JField("status", JString(WordStatus.fromValue(v.intValue()).name))
      case x => x
    }
  }

  def getWord(wid: ObjectId) = {
    wops.byId(wid).map {
      case Some(x) => JsonResponse(cleanWord(x))
      case None => NotFoundResponse()
    }
  }

  import ws.kotonoha.server.examples.api.JmdictJson._
  import ws.kotonoha.server.examples.api.ApiLift._

  def similarJm(wid: ObjectId): Future[LiftResponse] = {
    wops.byId(wid).map {
      case Some(x) =>
        val data = wjo.nsimilarForWord(x)
        val cleaned = JMDictUtil.cleanLanguages(data, LangUtil.langs.toSet)
        val jv = JLift.write(cleaned)
        JsonResponse(jv)
      case None => NotFoundResponse()
    }
  }

  def autoexamples(wid: ObjectId): Future[LiftResponse] = {
    wops.byId(wid).flatMap {
      case Some(x) =>
        weo.acquireExamples(x).map { ep =>
          JsonResponse(JLift.write(ep))
        }
      case None => Future.successful(NotFoundResponse())
    }
  }

}
