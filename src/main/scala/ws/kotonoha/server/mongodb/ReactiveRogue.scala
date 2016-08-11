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

import com.foursquare.rogue.MongoHelpers.MongoBuilder
import com.foursquare.rogue.{ModifyQuery, Query}
import com.mongodb.DBObject
import net.liftweb.mongodb.record.MongoRecord
import org.bson.{BSON => BSONDef}
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.buffer.ArrayReadableBuffer

import scala.concurrent.Future

/**
  * @author eiennohito
  * @since 2016/08/11
  */
trait ReactiveRogue extends ReactiveOpsAccess {
  def fetch[M <: MongoRecord[M], R]
    (q: Query[M, R, _])(implicit meta: ReactiveMongoMeta[M]): Future[List[R]] = {
    val coll = this.collection(q.collectionName)
    val obj = ReactiveRogue.rbson(q.asDBObject)
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

  def update[M <: MongoRecord[M]]
    (mq: ModifyQuery[M, _], upsert: Boolean = false, multiple: Boolean = false): Future[UpdateWriteResult] = {
    val q: Query[M, _, _] = mq.query
    val coll = this.collection(q.collectionName)
    val sobj = ReactiveRogue.rbson(q.asDBObject)
    val mobj = ReactiveRogue.rbson(MongoBuilder.buildModify(mq.mod))

    coll.update(sobj, mobj, upsert = upsert, multi = multiple)
  }

  def remove[M <: MongoRecord[M]]
  (q: Query[M, _, _], firstMatch: Boolean = false)(implicit meta: ReactiveMongoMeta[M]): Future[WriteResult] = {
    val coll = this.collection(meta.collectionName)
    val sobj = ReactiveRogue.rbson(q.asDBObject)

    coll.remove(sobj, firstMatchOnly = firstMatch)
  }
}

object ReactiveRogue {
  def rbson(o: DBObject): BSONDocument = {
    val bytes = BSONDef.encode(o)
    val rdr = ArrayReadableBuffer(bytes)
    val obj = BSONDocument.read(rdr)
    obj
  }
}
