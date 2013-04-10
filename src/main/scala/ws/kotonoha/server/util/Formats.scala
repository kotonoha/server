/*
 * Copyright 2012-2013 eiennohito
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

import net.liftweb.json._
import org.bson.types.ObjectId
import net.liftweb.json.JsonAST.JString
import scala.reflect.ClassTag

/**
 * @author eiennohito
 * @since 01.03.13 
 */

object OidSerializer extends Serializer[ObjectId] {
  override def serialize(implicit format: Formats) = {
    case x: ObjectId => JString(x.toString)
  }

  override def deserialize(implicit format: Formats) = {
    case (_, JString(s)) => new ObjectId(s)
  }
}

object EnumToStringSerializer {
  def instance[T <: Enumeration](meta: T)(implicit t: ClassTag[T]) = {
    new Serializer[meta.Value] {
      def deserialize(implicit format: Formats) = {
        case (_, JString(s)) => meta.withName(s)
      }
      def serialize(implicit format: Formats) = {
        case x: meta.Value => JString(x.toString)
      }
    }
  }
}
