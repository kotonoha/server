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

package ws.kotonoha.server.actors

import ws.kotonoha.server.qr.QrRenderer
import ws.kotonoha.server.records.QrEntry
import org.bson.types.ObjectId
import concurrent.duration.FiniteDuration

/**
 * @author eiennohito
 * @since 24.03.12
 */
trait QrMessage extends KotonohaMessage
case class CreateQr(data: String) extends QrMessage
case class CreateQrWithLifetime(data: String, lifetime: FiniteDuration) extends QrMessage


class QrCreator extends UserScopedActor {
  def registerObj(s: String, user: ObjectId): QrEntry = {
    val rend = new QrRenderer(s)
    val data = rend.toStream.toByteArray
    val obj = QrEntry.createRecord.user(user).content(s).binary(data)
    obj.save
  }

  def createQr(user: ObjectId, s: String) {
    val obj: QrEntry = registerObj(s, user)
    sender ! obj
  }

  def createQr(user: ObjectId, s: String, period: FiniteDuration) {
      val obj = registerObj(s, user)
      services ! RegisterLifetime(obj, period)
      sender ! obj
    }

  override def receive = {
    case CreateQr(data) => createQr(uid, data)
    case CreateQrWithLifetime(data, period) => createQr(uid, data, period)
  }
}
