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

package reactivemongo.akkastream

import akka.NotUsed
import akka.stream.scaladsl.Source
import reactivemongo.api.{Cursor, CursorProducer, WrappedCursor}
import reactivemongo.bson.BSONDocument
import ws.kotonoha.server.mongodb.MongoSourceStage

import scala.concurrent.ExecutionContext

/**
  * @author eiennohito
  * @since 2016/08/22
  */
object CursorSourceProducer extends CursorProducer[BSONDocument] {
  override type ProducedCursor = AkkaStreamingCursor
  override def produce(base: Cursor[BSONDocument]) = new AkkaStreamingCursor(base)
}

class AkkaStreamingCursor(val wrappee: Cursor[BSONDocument]) extends WrappedCursor[BSONDocument] {
  def source(limit: Int = Int.MaxValue)(implicit ec: ExecutionContext): Source[BSONDocument, NotUsed] = {
    Source.fromGraph(new MongoSourceStage(wrappee, limit))
  }
}
