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

import ws.kotonoha.server.actors.learning.WordsAndCards
import net.liftweb.json.JsonAST._
import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.json.{DefaultFormats, Extraction, JsonDSL}
import ws.kotonoha.server.records.{WordCardRecord, WordRecord}

/**
 * @author eiennohito
 * @since 04.02.12
 */

object ResponseUtil {

  import net.liftweb.json.JsonDSL._

  object Tr {
    def apply[T <: MongoRecord[T]](lst: List[T]): JArray = list2JList(lst)
    def apply[A](lst: List[A])(implicit tf: A => JValue): JArray = JsonDSL.seq2jvalue(lst)
  }

  def deuser(x: JValue) = {
    x.removeField {
      case JField("user", _) => true
      case _ => false
    }
  }

  def stripLift(x: JValue) = {
    x.transform {
      case JObject(JField("$dt" | "$oid", JString(s)) :: Nil) => JString(s)
    }
  }

  implicit def list2JList[T <: MongoRecord[T]](lst: List[T]): JArray = {
    JArray(lst.map(_.asJValue))
  }

  def jsonResponse(data: WordsAndCards): JValue = {
    implicit val format = DefaultFormats ++ Seq(OidSerializer)
    ("cards" -> Tr(data.cards)) ~
      ("words" -> Tr(data.words.map(w => WordRecord.trimInternal(w.asJValue)))) ~
      ("sequence" -> Extraction.decompose(data.sequence))
  }
}
