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

import net.liftweb.common.{Box, Failure}
import net.liftweb.mongodb.record.BsonRecord
import net.liftweb.mongodb.record.field.BsonRecordField
import reactivemongo.bson.{BSONDocument, BSONValue}
import ws.kotonoha.server.mongodb.{ReactiveBsonMeta, ReactiveBsonSupport}

/**
  * @author eiennohito
  * @since 2016/08/10
  */
class KotonohaBsonField[O <: BsonRecord[O], S <: BsonRecord[S]]
  (rec: O, meta: ReactiveBsonMeta[S])(implicit mf: Manifest[S])
  extends BsonRecordField[O, S](rec, meta) with ReactiveBsonSupport {

  override def rbsonValue = valueBox.toStream.map(meta.toRMong)

  override def fromRBsonValue(v: BSONValue) = v match {
    case d: BSONDocument =>
      val dflt = meta.createRecord
      setBox(meta.fillRMong(d, dflt))
    case _ =>
      Failure(s"$v should be a document")
  }

  def this(o: O, m: ReactiveBsonMeta[S], init: Box[S])(implicit mf: Manifest[S]) = {
    this(o, m)
    setBox(init)
  }
}
