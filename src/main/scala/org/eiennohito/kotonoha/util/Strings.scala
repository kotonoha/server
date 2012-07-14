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

package org.eiennohito.kotonoha.util

/**
 * @author eiennohito
 * @since 15.03.12
 */

object Strings {
  def substr(in: String, len: Int) = {
    val l = in.length()
    if (l <= len) in
    else in.substring(0, len) + "..."
  }

  def trim(in: String) = {
    var len = in.length
    var st = 0

    //japanese space or simple space
    while ((st < len) && (in(st) == ' ' || in(st) == '　')) {
      st += 1
    }
    while ((st < len) && (in(len - 1) == ' ' || in(len - 1) == '　')) {
      len -= 1
    }

    if (((st > 0) || (len < in.length))) in.substring(st, len) else in
  }
}
