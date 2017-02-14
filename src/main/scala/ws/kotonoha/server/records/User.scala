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

package ws.kotonoha.server.records

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.mongodb.record.field._
import net.liftweb.util.FieldError
import net.liftmodules.oauth.OAuthConsumer
import net.liftweb.record.field.{DateTimeField, EnumField, IntField, StringField}
import ws.kotonoha.server.util.{DateTimeUtils, UserUtil}
import net.liftweb.http.S
import net.liftweb.http.provider.HTTPCookie
import org.bson.types.ObjectId
import ws.kotonoha.server.KotonohaConfig
import ws.kotonoha.server.records.meta.{JodaDateField, KotonohaMongoRecord}

/**
 * @author eiennohito
 * @since 30.01.12
 */

object UserStatus extends Enumeration {
  type UserStatus = Value

  val Active, Banned, Demo = Value
}


class UserRecord private() extends MegaProtoUser[UserRecord] {

  import ws.kotonoha.server.util.KBsonDSL._

  def meta = UserRecord

  object username extends StringField(this, 50) {
    override def uniqueFieldId = Full("username")
  }

  object regDate extends JodaDateField(this)

  object apiPublicKey extends StringField(this, 32)

  object apiPrivateKey extends StringField(this, 32)

  object invite extends StringField(this, 32) {
    override def validate: List[FieldError] = {
      if (!AppConfig().inviteOnly.get) {
        return Nil
      }
      val v = this.get
      InviteRecord.find("key" -> v) match {
        case Full(_) => Nil
        case _ => List(FieldError(this, "Can't register user without invitation"))
      }
    }
  }

  object status extends EnumField(this, UserStatus, UserStatus.Active)

  override def save(safe: Boolean = true) = {
    val r = super.save(safe)
    InviteRecord.delete("key" -> invite.get)
    r
  }
}

object UserRecord extends UserRecord with MetaMegaProtoUser[UserRecord] with NamedDatabase with KotonohaMongoRecord[UserRecord] {
  def isAdmin = currentUser match {
    case Full(u) => u.superUser_?
    case _ => false
  }

  override def signupFields = username :: super.signupFields

  override def screenWrap = Full(<lift:surround with="default" at="content">
    <lift:bind/>
  </lift:surround>)

  override def skipEmailValidation = true

  def haveUser: Boolean = loggedIn_?

  def currentId = currentUserId map {
    new ObjectId(_)
  }

  override protected def userFromStringId(id: String) = currentId.flatMap(find(_))

  val authCookie = "koto-auth"

  private def deleteCookie() {
    val p = conPath
    val cook = HTTPCookie(authCookie, "").setPath(p)
    S.deleteCookie(cook)
  }

  private def conPath: String = {
    KotonohaConfig.safeString("context.path").getOrElse {
      if (S.contextPath == "") "/" else S.contextPath
    }
  }

  autologinFunc = Full({
    () =>
      for {
        cookie <- S.findCookie(authCookie).flatMap(_.value)
        ua <- S.request.flatMap(_.userAgent)
      } {
        UserUtil.authByCookie(cookie, ua) match {
          case Full(uid) =>
            logger.debug(s"autologging user $uid using cookie")
            updateCookie(uid)
            logUserIdIn(uid.toString)
          case _ =>
            logger.debug("invalid autologin cookie, cleaning it")
            deleteCookie()
        }
      }
  })


  onLogOut ::= {
    case _ => deleteCookie()
  }

  onLogIn ::= ((u: UserRecord) => {
    updateCookie(u.id.get)
  })

  override def findUserByUserName(email: String) = {
    super.findUserByUserName(email)
  }

  def checkUser(email: Box[String], pwd: Box[String]) = {
    val u = email.flatMap(findUserByUserName)
    u.map(_.testPassword(pwd)) match {
      case Full(true) => u
      case _ => Empty
    }
  }

  def updateCookie(id: ObjectId) {
    val s = UserUtil.cookieAuthFor(id, S.request.flatMap(_.userAgent).openOr("none"))
    val p = conPath
    S.addCookie(HTTPCookie(authCookie, s).setMaxAge(60 * 24 * 30 * 1000).setPath(p))
  }

  override def createRecord: UserRecord = super.createRecord
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

  object registeredDate extends JodaDateField(this)

  object status extends IntField(this, 0)

  def reset {}

  def enabled = status.get

  def user = null

  def consumerKey = apiPublic.get

  def consumerSecret = apiPrivate.get

  def title = name.get

  def applicationUri = null

  def callbackUri = null

  def xdatetime = registeredDate.get.toDate
}

object ClientRecord extends ClientRecord with KotonohaMongoRecord[ClientRecord] with NamedDatabase

class UserTokenRecord private() extends MongoRecord[UserTokenRecord] with ObjectIdPk[UserTokenRecord] {
  def meta = UserTokenRecord

  import DateTimeUtils._

  object user extends ObjectIdRefField(this, UserRecord)

  object label extends StringField(this, 100)

  object tokenPublic extends StringField(this, 32)

  object tokenSecret extends StringField(this, 32)

  object createdOn extends JodaDateField(this)

  def auth = AuthCode(AppConfig().baseUri.get, tokenPublic.get, tokenSecret.get)
}

object UserTokenRecord extends UserTokenRecord with MongoMetaRecord[UserTokenRecord] with NamedDatabase
