/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.ioc

import com.google.inject.{Provides, Singleton}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.{Await, ExecutionContext}

/**
  * @author eiennohito
  * @since 2016/08/08
  */
class RMongoModule extends ScalaModule {
  import ws.kotonoha.akane.config.ScalaConfig._
  import scala.concurrent.duration._

  override def configure() = {}

  @Provides
  @Singleton
  def driver(
    res: Res,
    cfg: Config
  ): MongoDriver = {
    val drv = MongoDriver(cfg)
    res.register(new AutoCloseable {
      override def close() = drv.close()
    })
    drv
  }

  @Provides
  @Singleton
  def data(
    drver: MongoDriver,
    cfg: Config
  )(implicit ec: ExecutionContext): DataMongo = {
    val uri = MongoConnection.parseURI(cfg.getString("mongo.uri")).get
    val conn = drver.connection(uri)
    val dbname = cfg.optStr("mongo.data").getOrElse("kotonoha")
    new DataMongo {
      override val connection = conn
      override val database = Await.result(conn.database(dbname), 1.minute)
    }
  }
}

trait DataMongo {
  def connection: MongoConnection
  def database: DefaultDB
  def collection(name: String): BSONCollection = database.collection[BSONCollection](name)
}
