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

package ws.kotonoha.server.examples

import com.google.protobuf.ByteString
import reactivemongo.bson.{BSONBinary, BSONHandler, BSONInteger, Macros, Subtype}
import ws.kotonoha.examples.api.{ExamplePack, ExampleSentence, PackStatus, SentenceUnit}

/**
  * @author eiennohito
  * @since 2016/08/12
  */
object ExamplesToBson {

  implicit object protoByteStringHandler extends BSONHandler[BSONBinary, ByteString] {
    override def write(t: ByteString) = BSONBinary(t.toByteArray, Subtype.GenericBinarySubtype)
    override def read(bson: BSONBinary) = {
      val arr = bson.value.readArray(bson.value.size)
      ByteString.copyFrom(arr)
    }
  }

  implicit object asdf extends BSONHandler[BSONInteger, PackStatus] {
    override def read(bson: BSONInteger) = PackStatus.fromValue(bson.value)
    override def write(t: PackStatus) = BSONInteger(t.value)
  }

  implicit val unitHandler = Macros.handler[SentenceUnit]
  implicit val seqHandler = Macros.handler[ExampleSentence]
  implicit val packHandler = Macros.handler[ExamplePack]
}
