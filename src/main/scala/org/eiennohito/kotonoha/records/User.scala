package org.eiennohito.kotonoha.records

import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field.LongPk
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.{EnumField, StringField}
import net.liftweb.common.Full

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
 * @since 30.01.12
 */

object UserStatus extends Enumeration {
  type UserStatus = Value

  val Active, Banned = Value
}

class UserRecord private() extends MegaProtoUser[UserRecord] {
  def meta = UserRecord

  object username extends StringField(this, 50) {
    override def uniqueFieldId = Full("username")
  }
  object apiPublicKey extends StringField(this, 32)
  object apiPrivateKey extends StringField(this, 32)
  object status extends EnumField(this, UserStatus, UserStatus.Active)
}

object UserRecord extends UserRecord with MetaMegaProtoUser[UserRecord] with NamedDatabase {

  override def signupFields = username :: super.signupFields

  override def screenWrap = Full(<lift:surround with="default" at="content">
			       <lift:bind /></lift:surround>)

  override def skipEmailValidation = true

  def currentId = currentUserId map {_.toLong}
}
