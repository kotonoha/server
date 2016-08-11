/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.actors

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef}
import com.mongodb.casbah.WriteConcern
import net.liftweb.mongodb.record.MongoRecord
import ws.kotonoha.server.mongodb.ReactiveMongoMeta

/**
 * @author eiennohito
 * @since 01.02.12
 */

case class SaveRecord[T <: MongoRecord[T]: ReactiveMongoMeta](rec: MongoRecord[T]) extends DbMessage {
  def meta = implicitly[ReactiveMongoMeta[T]]
}

case class UpdateRecord[T <: MongoRecord[T]](rec: MongoRecord[T]) extends DbMessage

case class DeleteRecord[T <: MongoRecord[T]](rec: MongoRecord[T]) extends DbMessage

case class RegisterMongo(mongo: ActorRef)

class MongoDBActor extends Actor with ActorLogging {
  override def receive = {
    case SaveRecord(rec) =>
      log.debug("saving object {}", rec)
      try {
        rec.save(WriteConcern.Acknowledged)
        sender ! true
      } catch {
        case e: Throwable => log.error(e, "Cant' save X"); sender ! Failure(e)
      }
    case UpdateRecord(rec) => rec.save(WriteConcern.Acknowledged); sender ! true
    case DeleteRecord(rec) => sender ! rec.delete_!
  }
}
