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

package ws.kotonoha.server.util

import net.liftweb.json.JsonAST.{JField, JObject, JValue}
import ws.kotonoha.lift.json.JWrite

/**
  * @author eiennohito
  * @since 2017/02/15
  */
object Jobj {

  trait JvalConv[T] {
    def value: JValue
  }

  implicit class JvalConvImpl[T](o: T)(implicit wr: JWrite[T]) extends JvalConv[T] {
    override def value: JValue = wr.write(o)
  }

  def apply(data: (String, JvalConv[_])*): JObject = JObject(
    data.map { case (name, fvl) => JField(name, fvl.value) }.toList
  )
}
