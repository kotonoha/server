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

package ws.kotonoha.server.tools

import scala.collection.JavaConversions._

/**
 * @author eiennohito
 * @since 19.10.12 
 */

object WordReadingMeaningToListPorter {
  import com.mongodb.casbah.Imports._

  private val re = "[,、･]".r

  private def parseItems(s: String): List[String] = {
    re.split(s).map { _.trim }.filter(_.length != 0).toList
  }

  def munch(o: AnyRef) = {
    o match {
      case s: String => parseItems(s)
      case l: List[String] => l
      case l: MongoDBList => l.toList.map(_.toString)
      case l: BasicDBList => l.toList.map(_.toString)
      case _ => Nil
    }
  }

  def process(item: MongoDBObject, key: String) = {
    val wl = item.get(key)
    wl foreach { i => item.put(key, MongoDBList(munch(i): _*)) }
  }

  def main(args: Array[String]) {
    val conn = MongoConnection()
    val koto = conn("kotonoha_prod")
    val tbl = koto("wordrecords")

    val everything = tbl.find()
    val processed = everything map {
      item => {
        //item("key")
        process(item, "writing")
        process(item, "reading")
        item
      }
    }

    processed foreach { tbl.save(_) }
  }
}
