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

package ws.kotonoha.server.qr

import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import javax.imageio.ImageIO
import java.io.{OutputStream, ByteArrayOutputStream}

/**
 * @author eiennohito
 * @since 24.03.12
 */

class QrRenderer(data: String) {
  val writer = new QRCodeWriter()
  val matrix = writer.encode(data, BarcodeFormat.QR_CODE, 300, 300)

  val img = MatrixToImageWriter.toBufferedImage(matrix)


  def toStream: ByteArrayOutputStream = {
    val bas = new ByteArrayOutputStream()
    toStream(bas)
    bas
  }

  def toStream(s : OutputStream) : OutputStream = {
    ImageIO.write(img, "PNG", s)
    s
  }
}
