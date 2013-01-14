package ws.kotonoha.server.mongodb

import net.liftweb.util.Props
import net.liftweb.mongodb.{MongoDB, MongoIdentifier, MongoMeta}
import com.mongodb.{ServerAddress, Mongo}
import com.typesafe.scalalogging.slf4j.Logging
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.bson.{Transformer, BSON}
import org.joda.time.DateTime
import ws.kotonoha.server.util.DateTimeUtils

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
 * @since 29.01.12
 */

object MongoDbInit extends Logging {
  var inited = false

  private val dateFromMongo = new Transformer {
    def transform(o: Any) = {
      o match {
        case x: java.util.Date => new DateTime(x, DateTimeUtils.UTC)
        case x: DateTime => x
        case x => x.asInstanceOf[AnyRef]
      }
    }
  }

  private val dateToMongo = new Transformer {
    def transform(o: Any) = {
      o match {
        case x: DateTime => x.toDate
        case _ => o.asInstanceOf[AnyRef]
      }
    }
  }

  def addHooks() {
    BSON.addDecodingHook(classOf[DateTime], dateFromMongo)
    BSON.addEncodingHook(classOf[DateTime], dateToMongo)
  }

  def init(): Unit = synchronized {
    if (!inited) {
      val server = Props.get("db.server").get
      val dbname = Props.get("db.name").get
      val dictname = Props.get("dict.name", "dict")

      logger.info(s"using on server $server database $dbname")

      val sa = new ServerAddress(server, Props.getInt("db.port", ServerAddress.defaultPort()))
      MongoDB.defineDb(DbId, new Mongo(sa), dbname)
      MongoDB.defineDb(DictId, new Mongo(sa), dictname)
      addHooks()
      inited = true
    }
  }
}

object DbId extends MongoIdentifier {
  val dbName = Props.get("db.name", "kotonoha")
  def jndiName = dbName
}

object DictId extends MongoIdentifier {
  val dbName = Props.get("dict.name", "dict")
  def jndiName = dbName
}

trait NamedDatabase { self: MongoMeta[_] =>
  override def mongoIdentifier = DbId
}

trait DictDatabase {self: MongoMeta[_] =>
  override def mongoIdentifier = DictId
}
