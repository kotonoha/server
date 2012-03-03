package org.eiennohito.kotonoha.web.snippet

import net.liftweb._
import http._
import json.DefaultFormats
import json.JsonAST.JField._
import json.JsonAST.JInt._
import json.JsonAST.{JInt, JDouble, JField}
import mongodb.JObjectParser
import sitemap.Loc.Snippet
import util._
import js._
import JsCmds._
import JE._
import xml.NodeSeq
import org.eiennohito.kotonoha.mongodb.mapreduce.DateCounter
import org.eiennohito.kotonoha.records.WordCardRecord

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
 * @since 03.03.12
 */

object XString {
  def unapply(in: Any): Option[String] = in match {
    case s: String => Some(s)
    case _ => None
  }
}

object XArrayNum {
  def unapply(in: Any): Option[List[Number]] =
  in match {
    case lst: List[_] => Some(lst.flatMap{case n: Number => Some(n) case _ => None})
    case _ => None
  }
}

object AllJsonHandler extends SessionVar[JsonHandler](
  new JsonHandler {
    import scala.collection.JavaConversions.iterableAsScalaIterable

    def loadDates: JsExp = {
      val res = WordCardRecord.useColl { col =>
          val cnt = new DateCounter()
          val cmd = cnt.command(col)
          val out = col.mapReduce(cmd)
          val v =  out.results().map (JObjectParser.serialize(_)(DefaultFormats)) reduce (_++_)

          v.transform {
            case JField("value", x) => JField("count", x \ "count")
            case JField("_id", y : JDouble) => JField("idx", JInt(y.values.toInt))
        }
      }
      res
    }

    def apply(in: Any): JsCmd = in match {
      case JsonCmd("loadDates", resp, _, _) =>
        Call(resp, loadDates)

      case JsonCmd("oneString", resp, XString(s), _) =>
        Call(resp, s)

      case JsonCmd("addOne", resp, XArrayNum(an), _) =>
        Call(resp, JsArray(an.map(n => Num(n.doubleValue + 1.0)) :_*))

      case _ => Noop
    }
  }
)

object ScheduledCount {
  def script(in: NodeSeq): NodeSeq =
    Script(AllJsonHandler.is.jsCmd &
             Function("loadDates", List("callback"),
                      AllJsonHandler.is.call("loadDates",
                                             JsVar("callback"),
                                             JsObj())))


}
