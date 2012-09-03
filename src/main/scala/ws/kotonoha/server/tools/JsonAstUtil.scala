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

package ws.kotonoha.server.tools

import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonAST.JField
import scala.Some
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonAST

/**
 * @author eiennohito
 * @since 03.09.12
 */


object JsonAstUtil {
  private def rec(in: JValue, saveArrays: Boolean): List[JValue] = {
    in match {
      case JObject(flds) => {
        val lst = flds.flatMap(rec(_, saveArrays))
        lst match {
          case Nil => Nil
          case x: List[JField] => List(JObject(x))
          case _ => Nil
        }
      }
      case JArray(x) => x.flatMap(rec(_, saveArrays)) match {
        case Nil if saveArrays => List(JArray(Nil))
        case Nil => Nil
        case v => List(JArray(v))
      }
      case JField(name, v) => rec(v, saveArrays) match {
        case Nil => Nil
        case x :: _ => List(JField(name, x))
      }
      case JNothing | JNull => Nil
      case x => List(x)
    }
  }

  def clean(in: JValue, saveArrays: Boolean = false): JValue = {
    rec(in, saveArrays) match {
      case Nil => JNothing
      case x :: _ => x
    }
  }
}
