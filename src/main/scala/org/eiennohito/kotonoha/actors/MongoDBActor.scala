package org.eiennohito.kotonoha.actors

import akka.actor.Actor
import net.liftweb.mongodb.record.MongoRecord

/*
 * Copyright 2012 eiennohito
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
/**
 * @author eiennohito
 * @since 01.02.12
 */

case class SaveRecord[T <: MongoRecord[T]](rec: MongoRecord[T])
case class UpdateRecord[T <: MongoRecord[T]](rec: MongoRecord[T])

class MongoDBActor extends Actor {
  protected def receive = {
    case SaveRecord(rec) => rec.save
    case UpdateRecord(rec) => rec.update
  }
}
