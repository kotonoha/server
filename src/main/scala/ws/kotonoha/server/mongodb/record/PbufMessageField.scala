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

import com.mongodb.{BasicDBObject, DBObject}
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.mongodb.record.field.MongoCaseClassField
import net.liftweb.record.Record
import org.bson.BSON
import reactivemongo.bson.buffer.{ArrayBSONBuffer, ArrayReadableBuffer}
import reactivemongo.bson.{BSONDocument, BSONHandler, BSONValue}
import ws.kotonoha.server.mongodb.ReactiveBsonSupport

/**
  * @author eiennohito
  * @since 2016/08/12
  */
class PbufMessageField[O <: Record[O], T](rec: O)(implicit mf: Manifest[T], bh: BSONHandler[BSONDocument, T])
  extends MongoCaseClassField[O, T](rec) with ReactiveBsonSupport {
  override def rbsonValue = valueBox.toStream.map(bh.write)

  override def fromRBsonValue(v: BSONValue) = v match {
    case d: BSONDocument =>
      val v = bh.readTry(d) match {
        case scala.util.Success(s) => Full(s)
        case scala.util.Failure(f) => Failure(s"error in deserialization of $mf", Full(f), Empty)
      }
      setBox(v)
    case _ => Failure(s"$v was not a document")
  }


  override def asDBObject: DBObject = {
    val xbson = bh.write(valueBox.openOrThrowException("it's okay"))
    val binary = BSONDocument.write(xbson, new ArrayBSONBuffer())
    val buf = binary.toReadableBuffer()
    val decode = BSON.decode(buf.readArray(buf.size))
    new BasicDBObject(decode.toMap)
  }

  override def setFromDBObject(dbo: DBObject): Box[T] = {
    val binary = BSON.encode(dbo)
    val doc = BSONDocument.read(ArrayReadableBuffer.apply(binary))
    fromRBsonValue(doc)
  }
}

object ProtobufByteStringSerializer
