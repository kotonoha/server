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

case class ConjObj(tag: String, content: String) {
  import Conjuable._
  def secondStem:Conjuable = {
    val godanEndings = Map (    
        "う"-> "い",
        "く"-> "き",
        "ぐ"-> "ぎ",
        "す"-> "し",
        "つ"-> "ち",
        "ぬ"-> "に",
        "ぶ"-> "び",
        "む"-> "み",
        "る"-> "り")
    def last(s:String) = s.substring(s.length - 1)
    tag match {
      case "vk" => "来"
      case _ if content.equals("する") => "し"
      case _ if content.equals("行く") => "行き"
      case "v1" => content.end("る", "")
      case s if s.startsWith("v5") =>  content.end(last(content), godanEndings.get(last(content)))
    }
    
  }
  def masuForm = secondStem conj "ます"
}

case class Conjuable(data: Option[String]) {
  def end(from: String, to: String) = {
    Conjuable(data flatMap {
      case s =>
        if (s.endsWith(from))
          Some(s.substring(0, s.length - from.length) + to)
        else None
    })
  }

  def end(from: String, to: Option[String]): Conjuable = if (to.isEmpty) Conjuable(None) else end(from, to.get)
  def end(pair:(String,  String)): Conjuable = end(pair._1, pair._2)


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

  def conj(rhs:String) = Conjuable(if(data.isEmpty) None else Some(data.get + rhs))
}

object Conjuable {
  implicit def string2Conjuable(in: String):Conjuable = new Conjuable(Some(in))

}

object ConjOk {
  def unapply(in: Conjuable) = in.data
}
