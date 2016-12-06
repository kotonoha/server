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

import java.nio.ByteBuffer
import java.security.SecureRandom

import net.liftweb.util.SecurityHelpers
import org.apache.commons.codec.binary.Hex
import org.apache.commons.lang3.StringUtils
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.engines.AESFastEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}
import ws.kotonoha.akane.io.Charsets

/**
 * @author eiennohito
 * @since 01.04.12
 */

object SecurityUtil {

  def makeArray(s: String): Array[Byte] = {
    Hex.decodeHex(StringUtils.leftPad(s, 32, '0').toCharArray.slice(0, 32))
  }

  def uriAesEncrypt(s: String, k: Array[Byte]): String = {
    val c = rawAesEncrypt(s, k)
    SecurityHelpers.base64EncodeURLSafe(c)
  }


  private val rng = new SecureRandom()

  def randomBytes(cnt: Int): Array[Byte] = {
    val arr = new Array[Byte](cnt)
    rng.nextBytes(arr)
    arr
  }

  def randomHex(bytes: Int = 16): String = {
    Hex.encodeHexString(randomBytes(bytes))
  }

  def encryptAes(s: String, key: Array[Byte]): String = {
    val processed: Array[Byte] = rawAesEncrypt(s, key)
    SecurityHelpers.base64Encode(processed)
  }


  def rawAesEncrypt(s: String, key: Array[Byte]): Array[Byte] = {
    val aes = new GCMBlockCipher(new AESFastEngine)
    val kp = new KeyParameter(key)
    val ivdata = randomBytes(16)
    val params = new ParametersWithIV(kp, ivdata)
    aes.init(true, params)
    val bytes = s.getBytes(Charsets.utf8)
    val cyphertext = new Array[Byte](aes.getOutputSize(bytes.length) + 16)
    val pos = aes.processBytes(bytes, 0, bytes.length, cyphertext, 16)
    aes.doFinal(cyphertext, 16 + pos)
    System.arraycopy(ivdata, 0, cyphertext, 0, 16)
    cyphertext
  }

  def decryptAes(enc: String, key: Array[Byte]): String = {
    try {
      val bytes = SecurityHelpers.base64Decode(enc)
      if (bytes.length < 16) return ""

      val aes = new GCMBlockCipher(new AESFastEngine)
      val kp = new KeyParameter(key)
      val params = new ParametersWithIV(kp, bytes, 0, 16)
      aes.init(false, params)
      val plaintext = new Array[Byte](aes.getOutputSize(bytes.length - 16))
      val len = aes.processBytes(bytes, 16, bytes.length - 16, plaintext, 0)
      val l1 = aes.doFinal(plaintext, len)
      val wrap = ByteBuffer.wrap(plaintext)
      wrap.limit(len + l1)
      val buf = Charsets.utf8.decode(wrap)
      new String(buf.array(), 0, buf.limit())
    } catch {
      case e: InvalidCipherTextException => "" //returns empty string on invalid cypher string
    }
  }
}
