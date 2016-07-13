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

import org.apache.commons.codec.binary.Hex
import net.liftweb.util.SecurityHelpers
import org.bouncycastle.crypto.engines.AESFastEngine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.InvalidCipherTextException
import org.apache.commons.lang3.StringUtils
import org.bouncycastle.crypto.modes.CFBBlockCipher
import ws.kotonoha.akane.io.Charsets

/**
 * @author eiennohito
 * @since 01.04.12
 */

object SecurityUtil {

  def makeArray(s: String) = {
    Hex.decodeHex(StringUtils.leftPad(s, 32, '0').toCharArray.slice(0, 32))
  }

  def uriAesEncrypt(s: String, k: Array[Byte]) = {
    val c = rawAesEncrypt(s, k)
    SecurityHelpers.base64EncodeURLSafe(c)
  }


  val rng = new SecureRandom()

  def randomHex(bytes: Int = 16) = {
    val array = new Array[Byte](bytes)
    rng.nextBytes(array)
    Hex.encodeHexString(array)
  }

  def encryptAes(s: String, key: Array[Byte]) = {
    val processed: Array[Byte] = rawAesEncrypt(s, key)
    SecurityHelpers.base64Encode(processed)
  }


  def rawAesEncrypt(s: String, key: Array[Byte]): Array[Byte] = {
    val aes = new PaddedBufferedBlockCipher(new CFBBlockCipher(new AESFastEngine(), 8))
    val kp = new KeyParameter(key)
    aes.init(true, kp)
    val bytes = s.getBytes("UTF-8")
    val processed = new Array[Byte](aes.getOutputSize(bytes.length))
    val pos = aes.processBytes(bytes, 0, bytes.length, processed, 0)
    aes.doFinal(processed, pos)
    processed
  }

  def decryptAes(enc: String, key: Array[Byte]) = {
    try {
      val aes = new PaddedBufferedBlockCipher(new CFBBlockCipher(new AESFastEngine(), 8))
      val kp = new KeyParameter(key)
      aes.init(false, kp)
      val bytes = SecurityHelpers.base64Decode(enc)
      val decr = new Array[Byte](aes.getOutputSize(bytes.length))
      val len = aes.processBytes(bytes, 0, bytes.length, decr, 0)
      val l1 = aes.doFinal(decr, len)
      new String(Charsets.utf8.decode(ByteBuffer.wrap(decr)).array(), 0, len + l1)
    } catch {
      case e: InvalidCipherTextException => "" //returns empty string on invalid cypher string
    }
  }
}
