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

package ws.kotonoha.server.model

import java.util.HashMap

import com.mongodb.MapReduceCommand
import com.mongodb.MapReduceCommand.OutputType
import net.liftweb.json.DefaultFormats
import net.liftweb.mongodb.JObjectParser
import org.bson.types.Code
import org.scalatest.Matchers
import ws.kotonoha.server.mongodb.MongoAwareTest
import ws.kotonoha.server.records.WordRecord

import scala.collection.JavaConversions

case class Result(idx: Int,  count: Double)

class MapReduceTest extends org.scalatest.FunSuite with Matchers with MongoAwareTest {
  import net.liftweb.mongodb.BsonDSL._
  implicit val formats = DefaultFormats
  test("simple mapreduce -- count users") {
    WordRecord.useColl { col =>
      val c = JObjectParser.parse("id" -> ("$ne" -> 0L))
      val map = """
      function Map() {
        emit(0, {count: ret1()})
      }
      """
      
      val reduce = """
      function Reduce(key, vals) {
        var x = {key: key, count: 0};
        vals.forEach( function(i) { x.count += i.count; } );
        return x;
      }"""

      val ret1 = "function ret1() { return 1; }"

      val scope = new HashMap[String, Object]()
      scope.put("ret1", new Code(ret1))

      val cmd = new MapReduceCommand(col, map, reduce, null, OutputType.INLINE, c)
      cmd.setScope(scope)
      val t = col.mapReduce(cmd)

      import JavaConversions.iterableAsScalaIterable

      val v =  t.results() map (JObjectParser.serialize(_))
      println(v)
    }
  }
}
