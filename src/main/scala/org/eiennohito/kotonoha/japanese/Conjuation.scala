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

package org.eiennohito.kotonoha.japanese

/**
 * @author eiennohito
 * @since 14.07.12
 */

case class ConjObj(tag: String, content: String)

case class Conjuable(data: Option[String]) {
  def end(from: String, to: String) = {
    Conjuable(data flatMap {
      case s =>
        if (s.endsWith(from))
          Some(s.substring(s.length - from.length) + to)
        else None
    })
  }

  def conj(s: String) = Conjuable(data map { _ + s})

  /**
   *
   * @param hm how many
   * @param f tranformation function
   * @return
   */
  def withLast(hm: Int)(f: String => Option[String]) = {
    Conjuable(data flatMap {s => {
      val l = s.length
      if (l < hm) { None }
      else {
        val rest = l - hm
        f(s.substring(rest)) map { s.substring(0, rest) + _ }
      }
    }})
  }
}

object Conjuable {
  implicit def string2Conjuable(in: String) = new Conjuable(Some(in))
}

object ConjOk {
  def unapply(in: Conjuable) = in.data
}
