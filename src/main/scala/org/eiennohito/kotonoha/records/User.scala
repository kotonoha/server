package org.eiennohito.kotonoha.records

import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.{EnumField, StringField}
import net.liftweb.common.Full
import net.liftweb.mongodb.record.field.{LongRefField, LongPk}
import net.liftweb.util.FieldError

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
  import org.eiennohito.kotonoha.util.KBsonDSL._
  def meta = UserRecord

  object username extends StringField(this, 50) {
    override def uniqueFieldId = Full("username")
  }
  object apiPublicKey extends StringField(this, 32)
  object apiPrivateKey extends StringField(this, 32)
  object invite extends StringField(this, 32) {
    override def validate : List[FieldError] = {
      if (!AppConfig().inviteOnly.is) {
        return Nil
      }
      val v = is
      InviteRecord.find("key" -> v) match {
        case Full(_) => Nil
        case _ => List(FieldError(this, "Can't register user without invitation"))
      }
    }
  }
  object status extends EnumField(this, UserStatus, UserStatus.Active)

  override def save = {
    val r = super.save
    InviteRecord.delete("key" -> invite.is)
    r
  }
}

object UserRecord extends UserRecord with MetaMegaProtoUser[UserRecord] with NamedDatabase {
  def isAdmin = currentUser match {
    case Full(u) => u.superUser_?
    case _ => false
  }

  override def signupFields = username :: super.signupFields

  override def screenWrap = Full(<lift:surround with="default" at="content">
			       <lift:bind /></lift:surround>)

  override def skipEmailValidation = true

  def currentId = currentUserId map {_.toLong}

  override protected def userFromStringId(id: String) = currentId.flatMap(find(_))
}

object ClientStatus extends Enumeration {
  type ClientStatus = Value

  val Active = Value
}

class ClientRecord private() extends MongoRecord[ClientRecord] with LongPk[ClientRecord] {
  def meta = ClientRecord

  object name extends StringField(this, 50)
  object apiPublic extends StringField(this, 32)
  object apiPrivate extends StringField(this, 32)
}

object ClientRecord extends ClientRecord with MongoMetaRecord[ClientRecord] with NamedDatabase

class UserTokenRecord private() extends MongoRecord[UserTokenRecord] with LongPk[UserTokenRecord] {
  def meta = UserTokenRecord

  object user extends LongRefField(this, UserRecord)
  object label extends StringField(this, 100)
  object tokenPublic extends StringField(this, 32)
  object tokenSecret extends StringField(this, 32)
}

object UserTokenRecord extends UserTokenRecord with MongoMetaRecord[UserTokenRecord] with NamedDatabase
