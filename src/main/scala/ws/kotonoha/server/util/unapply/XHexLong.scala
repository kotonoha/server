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

package ws.kotonoha.server.util.unapply

import net.liftweb.util.ControlHelpers._
import ws.kotonoha.server.util.ParseUtil
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 03.12.12 
 */
object XHexLong {
  def unapply(s: String): Option[Long] = {
    tryo {
      ParseUtil.hexLong(s)
    }
  }
}

object XLong {
  def unapply(s: String): Option[Long] = tryo { s.toLong }
}

object XInt {
  def unapply(s: String): Option[Int] = tryo { s.toInt }
}

object XOid {
  def unapply(s: String): Option[ObjectId] = {
    if (ObjectId.isValid(s)) Some(new ObjectId(s))
    else None
  }
}
