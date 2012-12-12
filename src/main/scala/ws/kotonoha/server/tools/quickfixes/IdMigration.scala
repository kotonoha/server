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

package ws.kotonoha.server.tools.quickfixes

/**
 * @author eiennohito
 * @since 09.12.12 
 */

import com.mongodb.casbah.Imports._

object IdMigration {
  val conn = MongoConnection()
  val db = conn("kotonoha_prod")

  def l2o(l: Long): ObjectId = {
    val i1 = l.toInt
    val i2 = (l >> 32).toInt
    new ObjectId(5000, i2, i1)
  }

  def migrateCol(col: MongoCollection, flds: List[String] = Nil) {
    var ids = Set[Long]()
    for (it <- col) {
      val id = it.get("_id")
      id match {
        case l1: java.lang.Long => {
          val l: Long = l1
          ids += l
          val objid = l2o(l)
          val c1 = it.put("_id", objid)
          flds.foreach(f => {
            val nfld = it.get(f)
            nfld match {
              case x: java.lang.Integer => {
                val oid = l2o(x.longValue())
                it.put(f, oid)
                Unit
              }
              case x: java.lang.Long => {
                val oid = l2o(x)
                it.put(f, oid)
                Unit
              }
              case x => {
                if (x == null) {
                  println("%s of %s was null!".format(f, objid))
                } else {
                  println("got item %s: %s of type %s in obj %s".format(f, x, x.getClass, objid))
                }
                Unit
              }
            }
          })
          val r1 = col.findAndRemove(MongoDBObject("_id" -> l1))
          col += it
          Unit
        }
        case _ => Unit
      }
    }
    //val rmq = MongoDBObject("_id" -> ("$in" -> MongoDBList(ids.toList)))
    println("Migrated %d records in coll %s, performing cleanup".format(ids.size, col.getFullName()))
    //col.findAndRemove(rmq)
  }

  def main(args: Array[String]) {
    migrateCol(db("addwordrecords"))
    migrateCol(db("changewordstatuseventrecords"), "word" :: "user" :: Nil)
    migrateCol(db("clientrecords"))
    migrateCol(db("markeventrecords"), "card" :: "user" :: Nil)
    migrateCol(db("ofelementrecords"), List("matrix"))
    migrateCol(db("ofmatrixrecords"), List("user"))
    migrateCol(db("userrecords"))
    migrateCol(db("usersettingss"))
    migrateCol(db("usertokenrecords"), "user" :: Nil)
    migrateCol(db("wordcardrecords"), "word" :: "user" :: Nil)
    migrateCol(db("wordrecords"), "user" :: Nil)
  }
}
