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

package ws.kotonoha.server.mongodb

import com.mongodb.{MongoClient, MongoClientOptions, MongoClientURI}
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.mongodb.{MongoDB, MongoMeta}
import net.liftweb.record.MetaRecord
import net.liftweb.util.ConnectionIdentifier
import org.bson.{BSON, Transformer}
import org.joda.time.DateTime
import ws.kotonoha.server.KotonohaConfig
import ws.kotonoha.server.util.DateTimeUtils

/**
 * @author eiennohito
 * @since 29.01.12
 */

object MongoDbInit extends Logging {
  @volatile private var inited = false

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

  def addHooks() = {
    BSON.addDecodingHook(classOf[DateTime], dateFromMongo)
    BSON.addEncodingHook(classOf[DateTime], dateToMongo)
  }

  @volatile private var client: MongoClient = null

  def init(): Unit = synchronized {
    if (!inited) {

      val dbname = KotonohaConfig.string("mongo.data")
      val dictname = KotonohaConfig.safeString("mongo.dict").getOrElse("dict")


      val co = new MongoClientOptions.Builder()
      val uri = new MongoClientURI(KotonohaConfig.string("mongo.uri"), co)

      logger.info(s"using on server ${uri.getHosts.get(0)}")

      client = new MongoClient(uri)

      try { client.fsync(false) } catch {
        case e: Exception =>
          e.printStackTrace()
      }

      register(client, dbname, dictname)
    }
  }

  def register(cliobj: MongoClient, dbname: String, dictname: String): Unit = {
    MongoDB.defineDb(DbId, cliobj, dbname)
    MongoDB.defineDb(DictId, cliobj, dictname)
    addHooks()
    inited = true
  }

  def stop(): Unit = {
    if (inited) {
      client.close()
      inited = false
    }
  }

  def ready = inited
}

object DbId extends ConnectionIdentifier {
  val dbName = KotonohaConfig.safeString("db.name").getOrElse("kotonoha")
  def jndiName = dbName
}

object DictId extends ConnectionIdentifier {
  val dbName = KotonohaConfig.safeString("dict.name").getOrElse("dict")
  def jndiName = dbName
}

trait NamedDatabase { self: MongoMeta[_] with MetaRecord[_] =>
  abstract override def connectionIdentifier: ConnectionIdentifier = DbId
}

trait DictDatabase {self: MongoMeta[_] with MetaRecord[_] =>
  abstract override def connectionIdentifier: ConnectionIdentifier = DictId
}
