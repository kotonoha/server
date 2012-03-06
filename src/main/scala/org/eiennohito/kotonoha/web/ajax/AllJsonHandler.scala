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

package org.eiennohito.kotonoha.web.ajax

import net.liftweb.http.{JsonHandler, SessionVar}
import org.eiennohito.kotonoha.mongodb.mapreduce.DateCounter
import net.liftweb.mongodb.JObjectParser
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST._
import net.liftweb.util.JsonCmd


import org.eiennohito.kotonoha.utls.Snippets._
import net.liftweb.http.js.JE._
import net.liftweb.http.js._

import JsCmds._
import org.eiennohito.kotonoha.web.snippet.{OFMatrix, ScheduledCount}
import org.eiennohito.kotonoha.records.{OFElementRecord, OFMatrixRecord, UserRecord, WordCardRecord}

object AllJsonHandler extends SessionVar[JsonHandler](
  new JsonHandler {

    import scala.collection.JavaConversions.iterableAsScalaIterable

    def loadDates: JsExp = {
      val res = WordCardRecord.useColl {
        col =>
          val cnt = new DateCounter()
          val cmd = cnt.command(col, UserRecord.currentId)
          val out = col.mapReduce(cmd)
          val v = out.results().map(JObjectParser.serialize(_)(DefaultFormats)) reduce (_ ++ _)

          v.transform {
            case JField("value", x) => JField("count", x \ "count")
            case JField("_id", y: JDouble) => JField("idx", JInt(y.values.toInt))
          }
      }
      res
    }

    def loadOFMatrix: JsExp = {
      import com.foursquare.rogue.Rogue._
      import org.eiennohito.kotonoha.utls.KBsonDSL._
      val matId = OFMatrixRecord.forUser(UserRecord.currentId.open_!).id.is
      val items = OFElementRecord where (_.matrix eqs matId) fetch()
      val data = items map { i => ("ef" -> i.ef) ~ ("n" -> i.n) ~ ("val" -> i.value) }
      JArray(data)
    }

    def apply(in: Any): JsCmd = in match {
      case JsonCmd(ScheduledCount.callbackName, resp, _, _) =>
        Call(resp, loadDates)

      case JsonCmd(OFMatrix.loadFncName, resp, _, _) =>
        Call(resp, loadOFMatrix)

      case JsonCmd("oneString", resp, XString(s), _) =>
        Call(resp, s)

      case JsonCmd("addOne", resp, XArrayNum(an), _) =>
        Call(resp, JsArray(an.map(n => Num(n.doubleValue + 1.0)): _*))

      case _ => JsCmds.Noop
    }
  }
)
