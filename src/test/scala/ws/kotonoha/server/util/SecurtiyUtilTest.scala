package ws.kotonoha.server.util

import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import javax.crypto.Cipher
import org.apache.commons.codec.binary.{Base64, Hex}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.modes.CFBBlockCipher
import org.bouncycastle.crypto.engines.AESFastEngine
import org.bouncycastle.crypto.params.KeyParameter


class SecurtiyUtilTest extends FreeSpec  with ShouldMatchers {
  "SecurityUtil" - {
    "encodes url like in java with aes" - {
      val key = "82e52a2bc0d78d206388788e897f7383"
      val msg = "My greatest message!"

      val binkey = Hex.decodeHex(key.toCharArray)
      val ks = new SecretKeySpec(binkey, "AES")
      val code = SecurityUtil.uriAesEncrypt(msg, binkey)
      val bytes = new Base64(true).decode(code)
      val aes = new PaddedBufferedBlockCipher(new CFBBlockCipher(new AESFastEngine(), 8))
      val kp = new KeyParameter(binkey)
      aes.init(false, kp)
      val decr = new Array[Byte](aes.getOutputSize(bytes.length))
      val len = aes.processBytes(bytes, 0, bytes.length, decr, 0)
      val l1 = aes.doFinal(decr, len)
      val resStr = new String(decr, 0, len + l1, "utf-8")
      resStr should equal (msg)
    }
  }
}
