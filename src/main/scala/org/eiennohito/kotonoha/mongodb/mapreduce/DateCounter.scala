package org.eiennohito.kotonoha.mongodb.mapreduce

import org.bson.types.Code
import org.eiennohito.kotonoha.utls.DateTimeUtils
import com.mongodb.{DBCollection, MapReduceCommand}
import com.mongodb.MapReduceCommand.OutputType
import net.liftweb.mongodb.JObjectParser
import net.liftweb.json.DefaultFormats

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
 * @since 28.02.12
 */

class DateCounter {
  import DateTimeUtils._
  import akka.util.duration._
  import scala.collection.JavaConversions.seqAsJavaList

  //requires dates value
  val index_in_range = new Code("""function rande_idx(val, arr){
    var len = arr.length;
    for (var i = 0; i < len; ++i) {
      var v = arr[i];
      if (v > val) {
        return i - 1;
      }
    }
    return len - 1;
  }""")

  val map = """function map() {
    var date = Math.max(this.notBefore, this.learning.intervalEnd);
    emit(range_idx(date, dates), {count : 1});
  }"""

  val reduce = """function reduce(key, vals) {
    var obj = {count: 0};
    vals.forEach(function(val) {
      obj.count += val.count;
    })
    return obj;
  }"""

  def dateList(): java.util.List[Long] =
    intervals(tonight, 1 day, 10) map { _.getMillis }

  val scope : java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]()
    map.put("range_idx", index_in_range)
    map.put("dates", dateList())
    map
  }

  def command(db: DBCollection) = {
    implicit val formats = DefaultFormats
    import net.liftweb.mongodb.BsonDSL._
    val date = d(now.plus(10 days))
    val q = ("notBefore" -> ("$lt" -> date)) ~ ("learning.intervalEnd" -> ("$lt" -> date))
    val cmd = new MapReduceCommand(db, map, reduce, null, OutputType.INLINE, JObjectParser.parse(q))
    cmd.setScope(scope)
    //cmd.addExtraOption("jsMode", true)
    cmd
  }
}
