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

package ws.kotonoha.server.mongodb

import reactivemongo.bson.{BSONReader, BSONValue}

/**
  * @author eiennohito
  * @since 2016/08/10
  */

trait ReactiveReader[T] {
  def read(b: BSONValue): T
}


object ReactiveReader {
  implicit def spawn[T](implicit internal: BSONReader[_ <: BSONValue, T]): ReactiveReader[T] = {
    new ReactiveReader[T] {
      private val rdr = internal.asInstanceOf[BSONReader[BSONValue, T]]
      override def read(b: BSONValue) = rdr.read(b)
    }
  }
}
