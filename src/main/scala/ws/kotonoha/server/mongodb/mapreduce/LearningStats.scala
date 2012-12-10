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

import ws.kotonoha.server.records.MarkEventRecord
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import ws.kotonoha.server.mongodb.{ProjectOperator, MatchOperator, GroupOperator}
import org.joda.time.DateTime
import net.liftweb.json.JsonAST._
import net.liftweb.mongodb.JObjectParser
import net.liftweb.json.{Extraction, DefaultFormats}
import net.liftweb.json.ext.JodaTimeSerializers
import net.liftweb.json.scalaz.JsonScalaz._
import scalaz.{Failure, Success}
import scala.Some
import net.liftweb.json.JsonAST.JArray
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JString

/**
 * @author eiennohito
 * @since 21.07.12
 */

case class RepeatStat(user: ObjectId, date: DateTime, avgMark: Double, total: Long)

object LearningStats extends GroupOperator with MatchOperator with ProjectOperator {
  import ws.kotonoha.server.util.DateTimeUtils._
  def recentLearning(days: Int) = {
    val dbo = MongoDBObject.newBuilder
    dbo += "aggregate" -> "markeventrecords"
    dbo += "pipeline" -> MongoDBList(
      $match ("datetime" $gt now.minusDays(10)),
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
    MarkEventRecord.useDb {db => {
      db.command(dbo.result())
    }}
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
      days map {d => d.dayOfMonth().get() -> d} toMap
    }
    jobj match {
      case JArray(a) => a flatMap {parseJson(_, day2Dates)}
      case _ => Nil
    }
  }

  def parseJson(in: JValue, day2Dates: Map[Int, DateTime]): List[RepeatStat] = {
    import scalaz._
    import Scalaz._

    import net.liftweb.json.scalaz.JsonScalaz._

    val id = in \ ("_id")

    def date(d: Int): Result[DateTime] = {
      day2Dates.get(d) match {
        case Some(c) => c.success
        case _ => Fail("date", "Have no such date")
      }
    }

    val ua = field[ObjectId]("user")(id)
    val da = field[Int]("day")(id) flatMap (date _)
    val ma = field[Double]("mark")(in)
    val mt = field[Long]("total")(in)
    val x: Result[RepeatStat] = (ua |@| da |@| ma |@| mt) { RepeatStat }
    x match {
      case Success(c) => List(c)
      case _ => Nil
    }
  }
}
