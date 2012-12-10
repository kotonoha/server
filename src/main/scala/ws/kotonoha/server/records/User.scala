package ws.kotonoha.server.records

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.common.Full
import net.liftweb.mongodb.record.field._
import net.liftweb.util.FieldError
import net.liftmodules.oauth.OAuthConsumer
import net.liftweb.record.field.{DateTimeField, EnumField, StringField}
import ws.kotonoha.server.util.{UserUtil, DateTimeUtils}
import net.liftweb.http.S
import net.liftweb.http.provider.HTTPCookie
import org.bson.types.ObjectId
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
  import ws.kotonoha.server.util.KBsonDSL._
  def meta = UserRecord

  object username extends StringField(this, 50) {
    override def uniqueFieldId = Full("username")
  }

  object regDate extends DateTimeField(this) with DateJsonFormat

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

  def currentId = currentUserId map { new ObjectId(_) }

  override protected def userFromStringId(id: String) = currentId.flatMap(find(_))

  private val authCookie = "koto-auth"

  private def deleteCookie() {
    val p = if (S.contextPath == "") "/" else S.contextPath
    val cook = HTTPCookie(authCookie, "").setPath(p)
    S.deleteCookie(cook)
  }

  autologinFunc = Full ({() =>
    S.findCookie(authCookie).
      flatMap(_.value).
      flatMap(crypt => {
        val ua = S.request.flatMap(_.userAgent).openOr("none")
        UserUtil.authByCookie(crypt, ua)
    }) match {
      case Full(uid) => {
        updateCookie(uid)
        logUserIdIn(uid.toString)
      }
      case _ => deleteCookie()
    }
  })


  onLogOut ::= { case _ => deleteCookie() }

  onLogIn ::= ((u: UserRecord) => {
    updateCookie(u.id.is)
  })

  def updateCookie(id: ObjectId) {
    val s = UserUtil.cookieAuthFor(id, S.request.flatMap(_.userAgent).openOr("none"))
    val p = if (S.contextPath == "") "/" else S.contextPath
    S.addCookie(HTTPCookie(authCookie, s).setMaxAge(60 * 24 * 30 * 1000).setPath(p))
  }
}

object ClientStatus extends Enumeration {
  type ClientStatus = Value

  val Active = Value
}

class ClientRecord private() extends MongoRecord[ClientRecord] with ObjectIdPk[ClientRecord] with OAuthConsumer {
  import DateTimeUtils._
  def meta = ClientRecord

  object name extends StringField(this, 50)
  object apiPublic extends StringField(this, 32)
  object apiPrivate extends StringField(this, 32)
  object registeredDate extends DateTimeField(this) with DateJsonFormat

  def reset {}

  def enabled = 0

  def user = null

  def consumerKey = apiPublic.is

  def consumerSecret = apiPrivate.is

  def title = name.is

  def applicationUri = null

  def callbackUri = null

  def xdatetime = registeredDate.is.toDate
}

object ClientRecord extends ClientRecord with MongoMetaRecord[ClientRecord] with NamedDatabase

class UserTokenRecord private() extends MongoRecord[UserTokenRecord] with LongPk[UserTokenRecord] {
  def meta = UserTokenRecord
  import DateTimeUtils._

  object user extends ObjectIdRefField(this, UserRecord)
  object label extends StringField(this, 100)
  object tokenPublic extends StringField(this, 32)
  object tokenSecret extends StringField(this, 32)
  object createdOn extends DateTimeField(this, now)

  def auth = AuthCode(AppConfig().baseUri.is, tokenPublic.is, tokenSecret.is)
}

object UserTokenRecord extends UserTokenRecord with MongoMetaRecord[UserTokenRecord] with NamedDatabase
