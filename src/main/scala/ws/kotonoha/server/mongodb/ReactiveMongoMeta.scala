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

import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.mongodb.record.{BsonMetaRecord, BsonRecord, MongoMetaRecord, MongoRecord}
import net.liftweb.record.MetaRecord
import reactivemongo.bson._

import scala.language.existentials
import scala.util.{Success, Try}


trait ReactiveBsonSupport {
  def rbsonValue: Stream[BSONValue]
  def fromRBsonValue(v: BSONValue): Box[_]
}

trait ReactiveBsonMeta[T <: BsonRecord[T]] extends BsonMetaRecord[T]  { self: T =>
  def toRMong(o: T): BSONDocument = {
    ReactiveBson.convertObj(o)
  }

  def fillRMong(doc: BSONDocument, o: T): Box[T] = {
    ReactiveBson.fillObj(o, doc).asInstanceOf[Box[T]]
  }

  implicit val bsonHandler: BSONDocumentHandler[T] = new BSONDocumentHandler[T] {
    override def read(bson: BSONDocument) = {
      val o = self.createRecord
      fillRMong(bson, o).openOrThrowException("read failed")
    }

    override def readTry(bson: BSONDocument): Try[T] = {
      val o = self.createRecord
      fillRMong(bson, o) match {
        case Full(f) => scala.util.Success(f)
        case Empty => scala.util.Failure(new ReadFailureException)
        case Failure(msg, exb, chain) =>
          val ex = ReadFailureException(msg, exb.openOr(null))
          var o = chain
          while (o.isDefined) {
            val obj = o.openOrThrowException("okay")
            ex.addSuppressed(ReadFailureException(obj.msg, obj.exception.openOr(null)))
            o = obj.chain
          }
          scala.util.Failure(ex)
      }
    }

    override def write(t: T) = toRMong(t)
  }
}

trait ReactiveMongoMeta[T <: MongoRecord[T]] extends MongoMetaRecord[T] with ReactiveBsonMeta[T] { self: MetaRecord[T] with T =>
  implicit def implicitaccess: ReactiveMongoMeta[T] = this
}


trait BSONDocumentHandler[T] extends BSONHandler[BSONDocument, T] with BSONDocumentWriter[T] with BSONDocumentReader[T]

case class ReadFailureException(msg: String, outer: Throwable) extends RuntimeException(msg, outer) {
  def this() = this("", null)
}
