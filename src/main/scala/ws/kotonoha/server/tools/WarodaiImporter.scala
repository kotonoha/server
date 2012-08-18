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
import java.io.{InputStreamReader, FileInputStream}
import collection.immutable.PagedSeq
import util.parsing.input.StreamReader
import ws.kotonoha.server.dict.WarodaiParser
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.dictionary.WarodaiRecord

/**
 * @author eiennohito
 * @since 05.04.12
 */

/**
 * Usage: first parameter is filename, second is arguments
 */
object WarodaiImporter extends App {
  MongoDbInit.init()
  val fn = args(0)
  val enc = args(1)

  val inp = new FileInputStream(fn)
  val reader = new InputStreamReader(inp, enc)
  val input = StreamReader(reader)
  val words = WarodaiParser.cards(input).get.toArray

  for (word <- words) {
    val rec = WarodaiRecord.createRecord
    val hdr = word.header
    val pos = hdr.id
    rec.posNum(pos.num).posPage(pos.page).posVol(pos.vol).
    readings(hdr.readings).writings(hdr.writings).rusReadings(hdr.rusReadings).
    body(word.body).save
  }
}
