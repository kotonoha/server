package org.eiennohito.kotonoha.util

import java.io.{Reader, StringReader, InputStreamReader}


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
 * @since 02.03.12
 */

object UnicodeUtil {
  val kanjiRanges = Array(0x4e00 -> 0x9fff,
                     0x3400 -> 0x4dbf,
                     0x20000 -> 0x2a6df,
                     0x2a700 -> 0x2b73f,
                     0x2b840 -> 0x2b81f)

  val hiraganaRange = Array(0x3040 -> 0x309f,
                          0x1b000 -> 0x1b0ff)

  val katakanaRange = Array(0x30a0 -> 0x30ff,
                            0xff60 -> 0xff96,
                            0x32d0 -> 0x32fe,
                            0x31f0 -> 0x31ff)

  def inRange(c: Int, ranges: Array[(Int, Int)]): Boolean = {
    ranges.foldLeft(false) {
      case (p, (begin, end)) => if (p) p else begin <= c && c <= end
    }
  }

  def isKanji(c: Int) = inRange(c, kanjiRanges)

  def isHiragana(p: Int) = inRange(p, hiraganaRange)

  def isKatakana(p: Int) = inRange(p, katakanaRange)

  def isKana(p: Int) = isHiragana(p) || isKatakana(p)

  def isJapanese(s: String) = stream(s).take(50).foldLeft(false) {
    case (false, c) => isKanji(c) || isHiragana(c) || isKatakana(c)
    case (true, _) => true
  }

  def stream(s: String): Stream[Int] = stream(new StringReader(s))

  def stream(r: Reader): Stream[Int] = {
      def rec(r : Reader) : Stream[Int] = {
        val read = r.read()
        if (read == -1) {
          return Stream.Empty
        }
        val c = read.toChar
        if (Character.isHighSurrogate(c)) {
          Stream.cons(Character.toCodePoint(c, r.read().toChar), rec(r))
        } else {
          Stream.cons(read, rec(r))
        }
      }
      rec(r)
    }

}
