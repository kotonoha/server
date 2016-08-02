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

package ws.kotonoha.server.util

import net.liftweb.common.{Empty, Full}
import unapply.{XOid, XHexLong}
import ws.kotonoha.server.records.AppConfig
import javax.crypto.KeyGenerator
import net.liftweb.util.SecurityHelpers
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 04.02.12
 */

trait UserUtilT {

  def serverKey: Array[Byte]

  def authByCookie(encrypted: String, userAgent: String) = {
    val str = SecurityUtil.decryptAes(encrypted, serverKey)
    str.split("\\|") match {
      case Array(XHexLong(time), XOid(uid), ua) =>
        val iua = SecurityHelpers.md5(userAgent)
        if (time > System.currentTimeMillis() && ua.equals(iua))
          Full(uid)
        else Empty
      case _ => Empty
    }
  }

  def cookieAuthFor(uid: ObjectId, userAgent: String) = {
    val date = DateTimeUtils.now.plusMonths(1).getMillis
    val ua = SecurityHelpers.md5(userAgent)
    val s = "%x|%s|%s".format(date, uid.toString, ua)
    SecurityUtil.encryptAes(s, serverKey)
  }

}

object UserUtil extends UserUtilT {
  override def serverKey = {
    AppConfig().myKey.valueBox match {
      case Full(k) => k
      case _ =>
        //val k = SecurityUtil.randomHex(16)
        val kgen = KeyGenerator.getInstance("AES")
        kgen.init(256)
        val k = kgen.generateKey().getEncoded
        AppConfig().myKey(k)
        AppConfig.save
        k
    }
  }
}
