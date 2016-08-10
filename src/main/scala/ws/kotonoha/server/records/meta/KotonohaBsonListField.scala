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

package ws.kotonoha.server.records.meta

import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.mongodb.record.field.BsonRecordListField
import reactivemongo.bson.{BSONArray, BSONDocument, BSONValue}
import ws.kotonoha.server.mongodb.{ReactiveBsonMeta, ReactiveBsonSupport}

/**
  * @author eiennohito
  * @since 2016/08/10
  */
class KotonohaBsonListField[O <: BsonRecord[O], S <: BsonRecord[S]]
  (rec: O, meta: ReactiveBsonMeta[S])(implicit mf: Manifest[S])
  extends BsonRecordListField[O, S](rec, meta) with ReactiveBsonSupport {

  override def rbsonValue = valueBox.toStream.map { l =>
    BSONArray(l.map(meta.toRMong))
  }

  override def fromRBsonValue(v: BSONValue) = {
    v match {
      case a: BSONArray =>
        var res: Box[Failure] = Empty
        val items = a.values.flatMap {
          case d: BSONDocument =>
            val o = meta.createRecord
            meta.fillRMong(d, o) match {
              case f: Failure =>
                res = Full(f.copy(chain = res))
                Stream.empty
              case Full(s) =>
                Stream(s)
              case _ =>
                Stream.empty
            }
          case x =>
            res = Full(Failure(s"can't support $x, should be an object", Empty, res))
            Stream.empty
        }
        if (res.isEmpty && items.nonEmpty) {
          setBox(Full(items.toList))
        } else res
      case _ =>
        Failure(s"$v is not supported, shout be array")
    }
  }
}
