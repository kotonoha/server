package org.eiennohito.kotonoha.model

import com.mongodb.MapReduceCommand.OutputType
import net.liftweb.json.JsonAST.JValue
import net.liftweb.mongodb.JObjectParser
import net.liftweb.json.DefaultFormats
import collection.JavaConversions
import org.eiennohito.kotonoha.records.{WordCardRecord, WordRecord, UserRecord}

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


class MapReduceTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with MongoDb {
  import net.liftweb.mongodb.BsonDSL._
  implicit val formats = DefaultFormats
  test("simple mapreduce -- count users") {
    WordRecord.useColl { col =>
      val c = JObjectParser.parse("id" -> ("$ne" -> 0L))
      val s = """
      function Map() {
        emit(0, {count: 1})
      }
      """
      
      val r = """
      function Reduce(key, vals) {
        var x = {key: key, count: 0};
        vals.forEach( function(i) { x.count += i.count; } );
        return x;
      }"""
      val t = col.mapReduce(s, r, null, OutputType.INLINE, c)

      import JavaConversions.iterableAsScalaIterable



      val v =  t.results() map (JObjectParser.serialize(_))
      println(v)
    }
  }
}