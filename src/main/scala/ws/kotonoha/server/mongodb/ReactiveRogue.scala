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

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.foursquare.rogue.MongoHelpers.MongoBuilder
import com.foursquare.rogue.{ModifyQuery, Query, SelectField}
import com.mongodb.{BasicDBObject, DBObject}
import net.liftweb.mongodb.record.MongoRecord
import org.bson.{BSON => BSONDef}
import reactivemongo.akkastream.CursorSourceProducer
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.bson.{BSONDocument, BSONInteger}
import reactivemongo.bson.buffer.{ArrayBSONBuffer, ArrayReadableBuffer}

import scala.concurrent.{ExecutionContext, Future}

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

    if (q.order.isDefined) {
      bldr = bldr.sort(ReactiveRogue.rbson(MongoBuilder.buildOrder(q.order.get)))
    }

    if (q.select.isEmpty) {
      bldr.cursor()
      val cursor = bldr.cursor()
      cursor.fold(List.newBuilder[R], limit) { (b, doc)  =>
        val rec = meta.createRecord
        b += meta.fillRMong(doc, rec).openOrThrowException("should not be empty").asInstanceOf[R]
      }.map(_.result())
    } else {
      val sel = q.select.get
      bldr = bldr.projection(ReactiveRogue.projection(sel.fields))
      val item = meta.createRecord
      val projector = ReactiveBson.projector(item, sel.fields)
      bldr.cursor().fold(List.newBuilder[R], limit) { (b, doc) =>
        b += sel.transformer(projector.project(doc).openOrThrowException("it's ok"))
      }.map(_.result())
    }
  }

  def stream[M <: MongoRecord[M], R]
    (q: Query[M, R, _], batchSize: Int = 0)(implicit meta: ReactiveMongoMeta[M]): Source[R, NotUsed] = {
    val coll = this.collection(q.collectionName)
    val obj = ReactiveRogue.rbson(q.asDBObject)
    val limit = q.lim.getOrElse(Int.MaxValue)
    var bldr = if (q.select.isEmpty) {
      coll.find(obj)
    } else {
      val sel = q.select.get
      coll.find(obj).projection(ReactiveRogue.projection(sel.fields))
    }

    if (batchSize != 0) {
      bldr = bldr.copy(options = bldr.options.copy(batchSizeN = batchSize))
    }

    if (q.order.isDefined) {
      bldr = bldr.sort(ReactiveRogue.rbson(MongoBuilder.buildOrder(q.order.get)))
    }

    implicit val cp = CursorSourceProducer

    val src = bldr.cursor().source(limit)
    val tf: (BSONDocument => R) = if (q.select.isEmpty) { (doc: BSONDocument) =>
      val rec = meta.createRecord
      meta.fillRMong(doc, rec).openOrThrowException("should not be empty").asInstanceOf[R]
    } else {
      val item = meta.createRecord
      val sel = q.select.get
      val projector = ReactiveBson.projector(item, sel.fields)
      (doc: BSONDocument) => sel.transformer(projector.project(doc).openOrThrowException("it's ok"))
    }
    src.map(tf)
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

  def count[M <: MongoRecord[M]](q: Query[M, _, _])(implicit ec: ExecutionContext): Future[Int] = {
    val coll = this.collection(q.collectionName)
    val sobj = ReactiveRogue.rbson(q.asDBObject)

    coll.count(Some(sobj))
  }
}

object ReactiveRogue {
  def projection(fields: List[SelectField[_, _]]): BSONDocument = {
    val flds = fields.map(p => p.field.name -> BSONInteger(p.slc.map(_._1).getOrElse(1)))
    BSONDocument(flds)
  }

  def rbson(o: DBObject): BSONDocument = {
    val bytes = BSONDef.encode(o)
    val rdr = ArrayReadableBuffer(bytes)
    val obj = BSONDocument.read(rdr)
    obj
  }

  def dbobj(bdoc: BSONDocument): DBObject = {
    val binary = BSONDocument.write(bdoc, new ArrayBSONBuffer())
    val buf = binary.toReadableBuffer()
    val decode = BSONDef.decode(buf.readArray(buf.size))
    new BasicDBObject(decode.toMap)
  }
}
