package org.eiennohito.kotonoha.model

import com.mongodb.MapReduceCommand.OutputType
import net.liftweb.mongodb.JObjectParser
import net.liftweb.json.DefaultFormats
import collection.JavaConversions
import org.eiennohito.kotonoha.records.{WordCardRecord, WordRecord, UserRecord}
import com.mongodb.MapReduceCommand
import java.util.HashMap
import org.bson.types.Code
import org.eiennohito.kotonoha.mongodb.mapreduce.DateCounter
import net.liftweb.json.JsonAST._

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


case class Result(idx: Int,  count: Double)

class MapReduceTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with MongoDb {
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

  test("mr2") {
    import JavaConversions.iterableAsScalaIterable

    WordCardRecord.useColl { col =>
      val cnt = new DateCounter()
      val cmd = cnt.command(col)
      val out = col.mapReduce(cmd)
      val v =  out.results() map (JObjectParser.serialize(_)) reduce (_++_)
      println(v)

      val transf = v.transform {
        case JField("value", x) => JField("count", x \ "count")
        case JField("_id", y : JDouble) => JField("idx", JInt(y.values.toInt))
      }.extract[List[Result]]

      println(transf)
    }
  }
}