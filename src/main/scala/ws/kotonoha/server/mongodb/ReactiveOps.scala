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

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.field.ObjectIdPk
import org.bson.types.ObjectId
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONObjectID}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/08/10
  */

trait ReactiveOps {
  protected implicit def ec: ExecutionContext
  protected def db: DefaultDB

  private[this] def collection(meta: ReactiveMongoMeta[_]): BSONCollection = {
    val collName = meta.collectionName
    db.collection[BSONCollection](collName)
  }

  def save[T <: MongoRecord[T]](objs: Seq[T], writeConcern: WriteConcern = WriteConcern.Acknowledged)(implicit meta: ReactiveMongoMeta[T]) = {
    val coll = collection(meta)
    val data = objs.map(meta.toRMong)
    val future = coll.bulkInsert(data.toStream, ordered = false, writeConcern = writeConcern)
    future
  }

  def byId[T <: MongoRecord[T] with ObjectIdPk[T]](id: ObjectId)(implicit meta: ReactiveMongoMeta[T]): Future[Option[T]] = {
    val coll = collection(meta)
    implicit val rdr: BSONDocumentReader[T] = meta.bsonHandler
    coll.find(BSONDocument("_id" -> BSONObjectID(id.toByteArray))).one[T]
  }

  def delete[T <: MongoRecord[T] with ObjectIdPk[T]](objs: Seq[T], writeConcern: WriteConcern = WriteConcern.Acknowledged)(implicit meta: ReactiveMongoMeta[T]) = {
    val coll = collection(meta)
    val ids = objs.map(x => BSONObjectID(x.id.get.toByteArray))
    val q = BSONDocument(
      "_id" -> BSONDocument(
        "$in" -> ids
      )
    )
    coll.remove(q, writeConcern)
  }
}

trait RMData extends ReactiveOps
