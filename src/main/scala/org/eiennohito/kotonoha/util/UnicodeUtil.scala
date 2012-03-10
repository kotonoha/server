package org.eiennohito.kotonoha.util

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
  val ranges = Array(0x4e00 -> 0x9fff,
                     0x3400 -> 0x4dbf,
                     0x20000 -> 0x2a6df,
                     0x2a700 -> 0x2b73f,
                     0x2b840 -> 0x2b81f)

  def isKanji(c: Int) = ranges.foldLeft(false) {
    case (p, (begin, end)) => if (p) p else begin <= c && c <= end
  }

}
