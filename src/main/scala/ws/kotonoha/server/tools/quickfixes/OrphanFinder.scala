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

package ws.kotonoha.server.tools.quickfixes

import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.{WordRecord, WordCardRecord}
import com.mongodb.casbah.WriteConcern

/**
 * @author eiennohito
 * @since 04.03.13 
 */

object OrphanFinder {
  MongoDbInit.init()

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def main(args: Array[String]) {

    println("Cards:")
    WordCardRecord select(_.id, _.word) foreach {
      case (cid, wid) =>
        val cnt = WordRecord where (_.id eqs wid) count()
        if (cnt == 0) println(cid)
    }

    println("Words:")
    WordRecord select (_.id) foreach {
      wid =>
        val cnt = WordCardRecord where (_.word eqs wid) count()
        if (cnt == 0) println(wid)
    }
  }
}

object OrphanedCardsCleaner {
  MongoDbInit.init()

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def main(args: Array[String]) {
    var count = 0
    WordCardRecord select(_.id, _.word) foreach {
      case (cid, wid) =>
        val cnt = WordRecord where (_.id eqs wid) count()
        if (cnt == 0) {
          WordCardRecord where (_.id eqs cid) bulkDelete_!! (WriteConcern.Normal)
          count += 1
        }
    }
    println(s"deleted $count cards")
  }
}
