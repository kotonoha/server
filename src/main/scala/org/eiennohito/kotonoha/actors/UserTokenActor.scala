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

import akka.actor.Actor
import org.eiennohito.kotonoha.records.UserTokenRecord
import org.eiennohito.kotonoha.util.SecurityUtil

/**
 * @author eiennohito
 * @since 25.03.12
 */

case class CreateTokenForUser(user: Long, label: String) extends TokenMessage

class UserTokenActor extends Actor with RootActor {

  def randomHex(bytes: Int = 16) = SecurityUtil.randomHex(bytes)

  def createToken(user: Long, label: String) {
    val token = UserTokenRecord.createRecord.
        user(user).label(label)
    val tokenPrivate = randomHex(16)
    val tokenPublic = randomHex(16)
    token.tokenPublic(tokenPublic).tokenSecret(tokenPrivate)
    root ! SaveRecord(token)
    sender ! token
  }

  protected def receive = {
    case CreateTokenForUser(user, label) => createToken(user, label)
  }
}
