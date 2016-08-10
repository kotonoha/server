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

package ws.kotonoha.server.mongodb.record

import net.liftweb.common.{Empty, Failure, Full}
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.mongodb.record.field.MongoListField
import reactivemongo.bson.{BSONArray, BSONValue}
import ws.kotonoha.server.mongodb.{ReactiveBson, ReactiveBsonSupport, ReactiveReader}

/**
  * @author eiennohito
  * @since 2016/08/10
  */
class BsonListField[O <: BsonRecord[O], T: Manifest](rec: O)(implicit rd: ReactiveReader[T]) extends MongoListField[O, T](rec) with ReactiveBsonSupport {
  override def rbsonValue = {
    valueBox.toStream.map { x =>
      BSONArray(x.map(y => ReactiveBson.convertValue(y)))
    }
  }

  override def fromRBsonValue(v: BSONValue) = {
    v match {
      case a: BSONArray =>
        try {
          val vals = a.values.map(v => rd.read(v))
          setBox(Full(vals.toList))
        } catch {
          case e: Exception =>
            Failure(s"could not deserialize $v", Full(e), Empty)
        }
      case _ =>
        Empty
    }
  }
}
