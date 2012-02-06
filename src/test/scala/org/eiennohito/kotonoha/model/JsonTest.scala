package org.eiennohito.kotonoha.model

import converters.DateTimeTypeConverter
import learning.Word
import org.eiennohito.kotonoha.records.{WordCardRecord, ExampleRecord, WordRecord}
import org.eiennohito.kotonoha.utls.ResponseUtil
import net.liftweb.json.{Printer, JsonAST}
import com.google.gson.{GsonBuilder, Gson}
import org.joda.time.DateTime
import net.liftweb.json.JsonAST.JObject


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
 * @since 31.01.12
 */

class JsonTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  test("word record becomes nice json") {
    val rec = WordRecord.createRecord
    rec.writing("hey").reading("guys")
    
    val js = rec.asJSON.toString()
    val rec2 = WordRecord.createRecord
    rec2.setFieldsFromJSON(js)
    rec.writing.is should equal (rec2.writing.is)
    rec.reading.is should equal (rec2.reading.is)
  }
  
  test("word record translates to java model") {
    val ex1 = ExampleRecord.createRecord.example("ex").translation("tr")
    val rec = WordRecord.createRecord
    rec.writing("wr").reading("re").meaning("me").examples(List(ex1)).id(5)

    val jv: JObject = rec.asJValue
    val str = Printer.pretty(JsonAST.render(ResponseUtil.deuser(jv)))

    println(str)

    val gb = new GsonBuilder
    gb.registerTypeAdapter(classOf[DateTime], new DateTimeTypeConverter)

    val gson = gb.create()
    val obj = gson.fromJson(str, classOf[Word])
    obj.getMeaning should equal ("me")
  }
}