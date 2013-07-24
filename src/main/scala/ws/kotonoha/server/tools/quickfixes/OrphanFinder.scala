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
