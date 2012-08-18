package ws.kotonoha.server.mongodb.mapreduce

import org.bson.types.Code
import ws.kotonoha.server.util.DateTimeUtils
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

  val map = """function map() {
    var tm = this.learning === null ? 0 : this.learning.intervalEnd.getTime();
    var date = Math.max(this.notBefore.getTime(), tm);
    var d = (date - pivot) / 86400000;
    var rng = Math.max(-1, Math.floor(d));
    emit(rng, {count : 1});
  }"""

  val reduce = """function reduce(key, vals) {
    var obj = {count: 0};
    vals.forEach(function(val) {
      obj.count += val.count;
    })
    return obj;
  }"""

  def dateList(): java.util.List[Long] =
    intervals(now, 1 day, 10) map { _.getMillis }

  val scope : java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]()
    map.put("pivot", System.currentTimeMillis().asInstanceOf[AnyRef])
    map
  }

  def command(db: DBCollection, uid: Option[Long] = None) = {
    implicit val formats = DefaultFormats
    import ws.kotonoha.server.util.KBsonDSL._
    val date = d(now.plus(10 days))
    val userq = uid map ("user" -> _)
    val q = ("notBefore" -> ("$lt" -> date)) ~ ("learning.intervalEnd" -> ("$lt" -> date)) ~ userq

    val cmd = new MapReduceCommand(db, map, reduce, null, OutputType.INLINE, JObjectParser.parse(q))
    cmd.setScope(scope)
    //cmd.addExtraOption("jsMode", true)
    cmd
  }
}
