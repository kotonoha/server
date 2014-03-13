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

import scalax.file.Path
import ws.kotonoha.akane.dict.kanjidic2.Kanjidic2Parser
import ws.kotonoha.server.mongodb.{DictId, MongoDbInit}
import net.liftweb.json.{DefaultFormats, Extraction}
import net.liftweb.mongodb.{MongoDB, JObjectParser}
import net.liftweb.json.JsonAST.JObject
import ws.kotonoha.server.records.dictionary.KanjidicRecord
import java.io.InputStream

/**
 * @author eiennohito
 * @since 17.01.13 
 */

object KanjidicImporter {
  implicit val formats = DefaultFormats

  def main(args: Array[String]) {
    MongoDbInit.init()
    val path = Path.fromString(args(0))
    for (is <- path.inputStream) {
      importKanjidic(is)
    }
  }

  def importKanjidic(is: InputStream) {
    val p = Kanjidic2Parser.parse(is)
    MongoDB.useCollection(DictId, KanjidicRecord.collectionName)(c => {
      c.drop()
      for (i <- p) {
        val jv = Extraction.decompose(i)
        val jo = JObjectParser.parse(jv.asInstanceOf[JObject])
        c.save(jo)
      }
    })
  }
}
