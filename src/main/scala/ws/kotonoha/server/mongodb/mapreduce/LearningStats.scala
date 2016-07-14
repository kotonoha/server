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

package ws.kotonoha.server.mongodb.mapreduce

import java.util

import com.mongodb.MapReduceCommand
import com.mongodb.MapReduceCommand.OutputType
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.casbah.query.Imports._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST.{JArray, JField, JString, _}
import net.liftweb.json.scalaz.JsonScalaz._
import net.liftweb.mongodb.JObjectParser
import org.bson.types.ObjectId
import org.joda.time.DateTime
import ws.kotonoha.server.records.events.MarkEventRecord
import ws.kotonoha.server.util.unapply.XOid

import scalaz.Success

/**
 * @author eiennohito
 * @since 21.07.12
 */

case class RepeatStat(user: ObjectId, date: DateTime, avgMark: Double, total: Long)

object LearningStats {
  import ws.kotonoha.server.util.DateTimeUtils._

  def recentLearning(days: Int) = {
    val dbo = MongoDBObject.newBuilder
    dbo += "aggregate" -> "markeventrecords"
    dbo += "pipeline" -> MongoDBList(
      MongoDBObject("$match" -> ("$datetime" $gt now.minusDays(days))),
      MongoDBObject(
        "$project" -> MongoDBObject(
          "_id" -> 0,
          "user" -> 1,
          "mark" -> 1,
          "day" -> MongoDBObject("$dayOfMonth" -> "$datetime")
        )
      ),
      MongoDBObject(
        "$group" -> MongoDBObject(
          "_id" -> MongoDBObject("user" -> "$user", "day" -> "$day"),
          "mark" -> MongoDBObject("$avg" -> "$mark"),
          "total" -> MongoDBObject("$sum" -> 1)
        )
      )
    )
    MarkEventRecord.useDb {
      db => {
        db.command(dbo.result())
      }
    }
  }

  implicit val objectIdJson = new JSONR[ObjectId] {
    def read(jv: JValue) = jv match {
      case JString(id) => Success(new ObjectId(id))
      case JObject(JField("$oid", JString(id)) :: Nil) => Success(new ObjectId(id))
      case _ => Fail("oid", "This is not ObjectId")
    }
  }

  def recent(top: Int) = {
    val mdb = recentLearning(top)
    val jobj = JObjectParser.serialize(mdb.get("result"))(DefaultFormats)
    val day2Dates = {
      val days = last10midn
      days map {
        d => d.dayOfMonth().get() -> d
      } toMap
    }
    jobj match {
      case JArray(a) => a flatMap {
        parseJson(_, day2Dates)(DefaultFormats)
      }
      case _ => Nil
    }
  }

  def parseJson(in: JValue, day2Dates: Map[Int, DateTime])(implicit formats: DefaultFormats): List[RepeatStat] = {

    val id = in \ "_id"
    val user = (id \ "user").extractOpt[ObjectId]
    val day = (id \ "day").extractOpt[Int].flatMap(day2Dates.get)
    val ma = (in \ "mark").extractOpt[Double]
    val mt = (in \ "total").extractOpt[Long]

    if (
      user.isDefined &&
      day.isDefined &&
      ma.isDefined &&
      mt.isDefined
    ) List(RepeatStat(
      user.get, day.get, ma.get, mt.get
    )) else Nil
  }

  def recentLearningMR(days: Int) = {
    val map = """
        function Map() {
         var u = this.user;
         var dist = Math.floor((now - this.datetime) / 1000 / 60 / 60 / 24);
         var marr = [0, 0, 0, 0, 0];
         marr[this.mark - 1] = 1;
         var o = {}
         o[dist] = marr;
         emit(u, o);
        }
              """

    val reduce =
      """
        function(key, vals) {
          var obj = vals[0];
          var i = 1, len = vals.length;
          for (; i < len; ++i) {
            var entry = vals[i];
            for (var objkey in entry) {
              var tmp = obj[objkey];
              if (tmp === undefined || tmp === null) {
                tmp = [0, 0, 0, 0, 0];
              }
              var data = entry[objkey];
              var pos = 0;
              for (; pos < 5; ++pos)
              {
                tmp[pos] += data[pos];
              }
              obj[objkey] = tmp;
            }
          }
          return obj;
        }
      """

    val midnight = now.withTimeAtStartOfDay()
    val date = midnight.minusDays(days).toDate
    val scope = DBObject("now" -> midnight.plusDays(1).getMillis)
    val q = "datetime" $gt date

    val res = MarkEventRecord.useColl(c => {
      val mrc = new MapReduceCommand(
        c,
        map,
        reduce,
        null,
        OutputType.INLINE,
        q
      )
      val smap = new util.HashMap[String, AnyRef]()
      val millis: java.lang.Long = midnight.plusDays(1).getMillis
      smap.put("now", millis)
      mrc.setScope(smap)
      c.mapReduce(mrc)
    })
    implicit val formats = DefaultFormats
    import scala.collection.JavaConversions._
    res.results().map(s => JObjectParser.serialize(s)).toList
  }

  case class UserMarks(user: ObjectId, reps: Map[Int, List[Int]])


  def transformMrData(in: List[JValue]): List[UserMarks] = {
    import scalaz._
    import Scalaz._
    //val ids = (in \\ "_id").
    implicit val formats = DefaultFormats

    def inner(in: JValue) = {
      //Extraction.extract[List[HiLvl]](in)
      val id = (in \ "_id").extractOpt[String].flatMap(XOid.unapply)
      val mp = (in \ "value").extractOpt[Map[String, List[Double]]].map {
        o => o.map {
          case (i, j) => i.toInt -> j.map(_.toInt)
        }
      }
      for {
        x1 <- id
        x2 <- mp
      } yield UserMarks(x1, x2)
    }

    in.flatMap(inner)
  }
}
