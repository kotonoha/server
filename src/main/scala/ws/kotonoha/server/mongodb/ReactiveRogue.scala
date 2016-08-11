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

import com.foursquare.rogue.Query
import net.liftweb.mongodb.record.MongoRecord
import org.bson.{BSON => BSONDef}
import reactivemongo.bson
import reactivemongo.bson.buffer.ArrayReadableBuffer

import scala.concurrent.Future

/**
  * @author eiennohito
  * @since 2016/08/11
  */
trait ReactiveRogue extends ReactiveOpsAccess {
  def fetch[M <: MongoRecord[M], R]
    (q: Query[M, R, _])(implicit meta: ReactiveMongoMeta[M]): Future[List[R]] = {
    val bytes = BSONDef.encode(q.asDBObject)
    val rdr = ArrayReadableBuffer(bytes)
    val obj = bson.BSONDocument.read(rdr)
    val coll = this.collection(q.collectionName)

    val limit = q.lim.getOrElse(Int.MaxValue)
    var bldr = coll.find(obj)
    val cursor = bldr.cursor()

    if (q.select.isEmpty) {
      cursor.fold(List.newBuilder[R], limit) { (b, doc)  =>
        val rec = meta.createRecord
        b += meta.fillRMong(doc, rec).openOrThrowException("should not be empty").asInstanceOf[R]
      }.map(_.result())
    } else ???
  }
}
