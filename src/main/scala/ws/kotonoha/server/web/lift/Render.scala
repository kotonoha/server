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

import org.bson.types.ObjectId

import scala.xml.{NodeSeq, Text}

/**
  * @author eiennohito
  * @since 2016/08/08
  */
trait Render[T] {
  def render(o: T): NodeSeq
}

object Renders {
  implicit object stringRender extends Render[String] {
    override def render(o: String) = Text(o)
  }

  implicit object intRender extends Render[Int] {
    override def render(o: Int) = Text(o.toString)
  }

  implicit object oidRender extends Render[ObjectId] {
    override def render(o: ObjectId) = Text(o.toHexString)
  }
}
