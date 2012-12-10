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

package ws.kotonoha.server.util

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FreeSpec
import javax.crypto.KeyGenerator
import ws.kotonoha.server.records.AppConfig
import net.liftweb.common.Full
import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 02.07.12
 */

class UserUtilTest extends FreeSpec with ShouldMatchers {

  val uu = new UserUtilT {
    lazy val serverKey = {
      val kgen = KeyGenerator.getInstance("AES")
      kgen.init(256)
      kgen.generateKey().getEncoded
    }
  }

  "cookie auth works" - {
    "test encrypt" in {
      val kgen = KeyGenerator.getInstance("AES")
      kgen.init(256)
      val k = kgen.generateKey().getEncoded
      val str = "welcome to the world of carnage!"
      val encr = SecurityUtil.encryptAes(str, k)
      //println (encr)
      SecurityUtil.decryptAes(encr, k) should equal (str)
    }

    "encrypts and decrypts uid" in {
      val oid = new ObjectId()
      val s = uu.cookieAuthFor(oid, "Firefox")
      val id = uu.authByCookie(s, "Firefox")
      id should equal (Full(oid))
    }
  }
}
