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

package ws.kotonoha.server.japanese

import net.liftweb.util.Props
import scalax.file.Path
import scalax.io._
import collection.mutable
import java.io.{BufferedReader, InputStreamReader}

/**
 * @author eiennohito
 * @since 20.08.12
 */

object Stopwords {

  def stopwords = if (Props.devMode) loadStopwords else stopwords_
  private lazy val stopwords_ = loadStopwords
  private def loadStopwords = {
    implicit val codec = Codec.UTF8
    val files = Props.get("various.stopwords", "")
    val paths = files.split(";").map(_.trim) filter(_.length > 5)
    val set = new mutable.HashSet[String]
    paths foreach (p => {
      val file = Path.fromString(p)
      for (is <- file.inputStream) {
        val rdr = new InputStreamReader(is, "utf-8")
        val br = new BufferedReader(rdr)
        var line: String = ""
        do {
          line = br.readLine()
          if (line != null && !line.startsWith("#")) {
            set += line
          }
        } while (line != null)
      }
    })
    set remove("")
    set toSet
  }
}
