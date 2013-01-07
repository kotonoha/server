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

package ws.kotonoha.server.actors.auth

import akka.actor.Actor
import ws.kotonoha.server.records.ClientRecord
import akka.pattern.{ask}
import akka.util.duration._
import ws.kotonoha.server.actors.{KotonohaActor, ClientMessage, SaveRecord, RootActor}

/**
 * @author eiennohito
 * @since 24.03.12
 */

case class AddClient(name: ClientRecord) extends ClientMessage

class ClientRegistry extends KotonohaActor {
  import ws.kotonoha.server.util.SecurityUtil._

  def addClient(rec: ClientRecord) = {
    val r1 = rec.apiPrivate(randomHex()).apiPublic(randomHex())
    val s = sender
    ask(services, SaveRecord(r1))(5 seconds) foreach {x => s ! r1}
  }

  protected def receive = {
    case AddClient(cl) => addClient(cl)
  }
}
