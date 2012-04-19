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

package org.eiennohito.kotonoha.dict

import java.io.File
import java.nio.ByteBuffer

/**
 * @author eiennohito
 * @since 19.04.12
 */

case class TatoebaLink(left: Long, right: Long, leftLang: String, rightLang: String) {
  def toBuffer(in: ByteBuffer) = {
    import TatoebaLink.intC
    in.putInt(left.toInt)
    in.putInt(intC(leftLang).toInt)
    in.putInt(right.toInt)
    in.putInt(intC(rightLang).toInt)
  }
}

object TatoebaLink {
  def fromBuffer(in: ByteBuffer) = {
    val left = in.getInt
    val leftLang = cInt(in.getInt)
    val right = in.getInt
    val rightLang = cInt(in.getInt)
    TatoebaLink(left, right, leftLang, rightLang)
  }

  def intC(in: String) = {
    var l = 0
    var i = 0
    val length = in.length min 4
    while (i < length) {
      l |= (in(i).toInt << (8 * i))
      i += 1
    }
    l
  }

  def cInt(in: Long) = {
    val chars = new Array[Char](4)
    var i = 3
    while (i >= 0) {
      chars(i) = ((in >> (i * 8)) & 0xff).toChar
      i -= 1
    }
    val idx = chars.indexOf('\0')
    new String(chars, 0, if (idx == -1) 4 else idx)
  }
}

class TatoebaLinks(in: File) {

  //private val
}
