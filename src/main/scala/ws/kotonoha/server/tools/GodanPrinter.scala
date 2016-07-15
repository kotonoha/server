/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.tools

import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.dictionary.JMDictRecord
import collection.mutable

/**
 * @author eiennohito
 * @since 21.01.13 
 */

case class Entry(w: String, r: String)

object GodanPrinter {

  import ws.kotonoha.server.util.KBsonDSL._

  def main(args: Array[String]) {
    //{"writing.value": /.*[えけせてめれねげぜでべぺへいきしちみりにぎじぢひぴび]る/, "meaning.info": "v5r"}
    val p = ".*[いきしちにひみりぎじぢびぴえけせてねべめれげぜでべぺ]る".r
    val req = ("reading.value" -> p) ~ ("meaning.info" -> "v5r")
    MongoDbInit.init()
    val data = JMDictRecord.findAll(req)
    val x = data.flatMap {
      i =>
        val w = i.writing.is.headOption
        val r = i.reading.is.headOption
        (w, r) match {
          case (Some(w), Some(r)) => List(Entry(w.value.is, r.value.is))
          case _ => Nil
        }
    }
    val s = new mutable.LinkedHashSet[String]
    x.sortBy(_.w.length).filter {
      i =>
        val c = s.exists(s => i.w.contains(s))
        if (c) false
        else {
          s += i.w
          true
        }
    }.sortBy(_.r).foreach(e => println(s"${e.w} (${e.r})"))
  }
}
