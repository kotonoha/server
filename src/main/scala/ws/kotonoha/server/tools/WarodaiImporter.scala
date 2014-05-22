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

import java.io.{BufferedReader, InputStreamReader, FileInputStream}
import ws.kotonoha.server.dict.{WarodaiCard, WarodaiParser}
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.dictionary.WarodaiRecord
import scala.util.parsing.input.StreamReader
import ws.kotonoha.akane.unicode.UnicodeUtil
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author eiennohito
 * @since 05.04.12
 */

/**
 * Usage: first parameter is filename, second is arguments
 */
object WarodaiImporter extends Logging {

  def main(args: Array[String]) {
    MongoDbInit.init()
    val fn = args(0)
    val enc = args(1)

    importWarodai(fn, enc)
  }

  def importWarodai(filename: String, encoding: String) = {
    val inp = new FileInputStream(filename)
    val reader = new BufferedReader(new InputStreamReader(inp, encoding))

    var skipping = true
    while (skipping) {
      reader.mark(1024)
      val line = reader.readLine()
      if (UnicodeUtil.hasKana(line) || UnicodeUtil.hasKanji(line)) {
        reader.reset()
        skipping = false
      }
    }

    val input = StreamReader(reader)

    val words = WarodaiParser.cards(input) match {
      case WarodaiParser.Success(words, _) =>
        saveWords(words)
      case f =>
        logger.warn(s"Unable to parse warodai dictionarty\n$f")
    }

  }

  def saveWords(words: List[WarodaiCard]): Any = {
    for (word <- words) {
      val rec = WarodaiRecord.createRecord
      val hdr = word.header
      val warId = hdr.id
      rec.warodaiId(warId).
        readings(hdr.readings).writings(hdr.writings).rusReadings(hdr.rusReadings).
        body(word.body).save
    }
  }
}
