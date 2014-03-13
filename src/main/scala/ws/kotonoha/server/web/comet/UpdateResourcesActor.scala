/*
 * Copyright 2012-2013 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.web.comet

import net.liftweb.http.CometActor
import ws.kotonoha.server.actors.lift.NgLiftActor
import net.liftweb.json.JsonAST.{JString, JValue}
import com.typesafe.scalalogging.slf4j.Logging
import java.net.{URI, URL}
import scalax.file.Path
import resource.Resource
import java.util.zip.GZIPInputStream
import scalax.io.StandardOpenOption
import org.apache.commons.io.IOUtils
import ws.kotonoha.server.tools.JMDictImporter

/**
 * @author eiennohito
 * @since 2014-03-13
 */
class UpdateResourcesActor extends CometActor with NgLiftActor with Logging {
  import ws.kotonoha.server.util.KBsonDSL._
  import ws.kotonoha.server.util.DateTimeUtils.now
  import Resource._

  val self = this

  override def receiveJson = {
    case x => self ! x
  }

  override def svcName = "UpdateResourcesSvc"

  def processJMDict(dir: Path, uri: URI): Unit = {
    val conn = uri.toURL.openConnection()
    for (fs <- resource.managed(conn.getInputStream)) {
      val gz = new GZIPInputStream(fs)
      val outFile = dir / "jmdict"
      message(s"downloading jmdict to $outFile")
      for (outfs <- outFile.outputStream(StandardOpenOption.Create)) {
        val size = IOUtils.copy(gz, outfs)
        message(s"successfully downloaded jmdict: $size bytes")
      }
    }
    for (input <- (dir / "jmdict").inputStream()) {
      message("importing jmdict")
      JMDictImporter.process(input)
      message("finished importing jmdict")
    }
  }

  def startImport(): Unit = {
    //download jmdict

    val dir = Path.createTempDirectory(prefix = "kotonoha", suffix = "import")

    message(s"using $dir as temporary directory")

    val jmdictUrl = URI.create("ftp://ftp.monash.edu.au/pub/nihongo/JMdict.gz")

    processJMDict(dir, jmdictUrl)

  }

  def processJson(jv: JValue): Unit = {
    jv \ "cmd" match {
      case JString("start") => startImport()
    }
  }

  override def lowPriority = {
    case jv: JValue => processJson(jv)
  }

  def message(msg: String) = {
    logger.info(msg)
    ngMessage(("date" -> now) ~ ("message" -> msg))
  }
}
