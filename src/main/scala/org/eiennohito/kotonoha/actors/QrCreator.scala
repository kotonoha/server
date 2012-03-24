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

package org.eiennohito.kotonoha.actors

import org.eiennohito.kotonoha.qr.QrRenderer
import akka.actor.{ActorRef, Actor}
import org.eiennohito.kotonoha.records.QrEntry

/**
 * @author eiennohito
 * @since 24.03.12
 */
trait QrMessage extends KotonohaMessage

case class CreateQr(user: Long, data: String) extends QrMessage

class QrCreator extends Actor {
  import akka.util.duration._
  lazy val root = context.actorFor("root")

  def createQr(user: Long, s: String) {
    val rend = new QrRenderer(s)
    val data = rend.toStream.toByteArray
    val obj = QrEntry.createRecord.user(user).content(s).binary(data)
    root ! SaveRecord(obj)
    root ! RegisterLifetime(obj, 1 day)
    sender ! obj
  }

  var mongo : ActorRef = _

  protected def receive = {
    case CreateQr(user, data) => createQr(user, data)
  }
}
