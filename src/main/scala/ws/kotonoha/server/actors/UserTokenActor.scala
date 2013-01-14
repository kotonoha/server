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

import akka.actor.Actor
import ws.kotonoha.server.records.UserTokenRecord
import ws.kotonoha.server.util.SecurityUtil
import org.bson.types.ObjectId
import net.liftweb.json
import json.{DefaultFormats, Extraction}

/**
 * @author eiennohito
 * @since 25.03.12
 */

case class CreateToken(user: ObjectId, label: String) extends TokenMessage
case class EncryptedTokenString(req: CreateToken, key: String) extends TokenMessage

class UserTokenActor extends Actor {
  import ws.kotonoha.server.util.DateTimeUtils._

  def randomHex(bytes: Int = 16) = SecurityUtil.randomHex(bytes)

  def createToken(user: ObjectId, label: String) {
    sender ! makeToken(user, label)
  }


  def makeToken(user: ObjectId, label: String): UserTokenRecord = {
    val token = UserTokenRecord.createRecord.
      user(user).label(label).createdOn(now)
    val tokenPrivate = randomHex(16)
    val tokenPublic = randomHex(16)
    token.tokenPublic(tokenPublic).tokenSecret(tokenPrivate)
    token.save
    token
  }


  implicit val formats = DefaultFormats
  def createEncryptedString(req: CreateToken, key: String) = {
    val data = makeToken(req.user, req.label)
    val raw = json.compact(json.render(Extraction.decompose(data.auth)))
    val encKey = SecurityUtil.makeArray(key)
    val obj = SecurityUtil.uriAesEncrypt(raw, encKey)
    sender ! obj
  }

  override def receive = {
    case CreateToken(user, label) => createToken(user, label)
    case EncryptedTokenString(req, key) => createEncryptedString(req, key)
  }
}
