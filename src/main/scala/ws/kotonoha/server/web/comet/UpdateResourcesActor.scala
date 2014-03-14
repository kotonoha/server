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
import ws.kotonoha.server.actors.lift.{AkkaInterop, NgLiftActor}
import net.liftweb.json.JsonAST.JValue
import com.typesafe.scalalogging.slf4j.Logging
import java.net.{URI, URL}
import scalax.file.Path
import resource.Resource
import java.util.zip.{ZipFile, GZIPInputStream}
import scalax.io.StandardOpenOption
import org.apache.commons.io.IOUtils
import ws.kotonoha.server.tools._
import ws.kotonoha.server.KotonohaConfig
import scala.Some
import net.liftweb.json.JsonAST.JString
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import ws.kotonoha.server.actors.{ReloadLucene, CloseLucene}
import ws.kotonoha.server.actors.dict.{LoadExampleIndex, CloseExampleIndex}
import net.liftweb.http.js.JsCmds.Reload
import net.liftweb.common.Full
import scala.concurrent.Future

/**
 * @author eiennohito
 * @since 2014-03-13
 */
class UpdateResourcesActor extends CometActor with NgLiftActor with AkkaInterop with Logging with ReleaseAkka {

  import ws.kotonoha.server.util.KBsonDSL._
  import ws.kotonoha.server.util.DateTimeUtils.now
  import Resource._

  val self = this

  override def receiveJson = {
    case x => self ! x
  }

  override def svcName = "UpdateResourcesSvc"

  def processJMDict(dir: Path, uri: URI): Unit = {
    val outFile = dir / "jmdict.xml.gz"
    download(uri.toURL, outFile)
    for (input <- outFile.inputStream()) {
      message("importing jmdict")
      JMDictImporter.process(new GZIPInputStream(input))
      message("finished importing jmdict")
    }
  }

  def download(url: URL, path: Path) = {
    message(s"downloading $url to $path")
    for (ifs <- resource.managed(url.openStream());
         ofs <- path.outputStream(StandardOpenOption.Create)) {
      IOUtils.copy(ifs, ofs)
      message(s"downloading $url succeeded")
    }
  }

  def processTatoeba(dir: Path, sentsUrl: String, linksUrl: String, tagsUrl: String) = {
    toAkka(CloseLucene)
    toAkka(CloseExampleIndex)

    val sentences = dir / "sentences.csv"
    download(new URL(sentsUrl), sentences)

    val links = dir / "links.csv"
    download(new URL(linksUrl), links)

    val tags = dir / "tags.csv"
    download(new URL(tagsUrl), tags)

    message("importing tags and sentences to mongo")
    TatoebaSentenceImporter.importTaggedSentences(tags, sentences)
    message("finished importing tags and sentences")

    KotonohaConfig.safeString("lucene.indexdir") match {
      case None => message("ERROR: lucene.indexdir is not configured! please reconfigure paths")
      case Some(ld) =>
        message("creating lucene index for example sentences")
        LuceneExampleIndexer.createIndex(ld)
        message("index was successuflly created")
    }

    toAkka(ReloadLucene)

    KotonohaConfig.safeString("example.index") match {
      case None => message("ERROR: example.index is not configured!")
      case Some(ei) =>
        message("creating example link index")
        TatoebaLinkParser.produceExampleLinks(links, sentences, Path.fromString(ei))
    }

    toAkka(LoadExampleIndex)
  }

  def processKanjidic(dir: Path) = {
    val url = "http://www.csse.monash.edu.au/~jwb/kanjidic2/kanjidic2.xml.gz"

    val file = dir / "kanjidic.xml.gz"

    download(new URL(url), file)

    for (is <- file.inputStream()) {
      val stream = new GZIPInputStream(is)
      message("importing kanjidic")
      KanjidicImporter.importKanjidic(stream)
      message("kanjidic imported")
    }
  }

  def processWarodai(dir: Path) = {
    val url = "http://e-lib.ua/dic/download/ewarodai.zip"

    val archive = dir / "warodai.zip"
    val textfile = dir / "warodai.txt"

    download(new URL(url), archive)

    val zip = new ZipFile(archive.path)
    val entry = zip.getEntry("ewarodai.txt")
    if (entry == null) {
      message("ERROR: no ewarodai.txt in archive")
    } else {
      for {
        is <- resource.managed(zip.getInputStream(entry))
        os <- textfile.outputStream(StandardOpenOption.Create)
      } {
        IOUtils.copy(is, os)
      }
    }

    message("importing warodai")
    WarodaiImporter.importWarodai(textfile.path, "UTF-16LE")
    message("imported warodai")
  }

  def startImport(): Unit = {
    //download jmdict

    val dir = Path.createTempDirectory(prefix = "kotonoha", suffix = "import")

    message(s"using $dir as temporary directory")

    val jmdictUrl = URI.create("ftp://ftp.monash.edu.au/pub/nihongo/JMdict.gz")

    processJMDict(dir, jmdictUrl)

    processTatoeba(dir,
      "http://tatoeba.org/files/downloads/sentences.csv",
      "http://tatoeba.org/files/downloads/links.csv",
      "http://tatoeba.org/files/downloads/tags.csv")

    processKanjidic(dir)

    processWarodai(dir)

    message("finished processing resources")

  }

  implicit val ec = akkaServ.context

  def processJson(jv: JValue): Unit = {
    jv \ "cmd" match {
      case JString("start") =>
        val f = Future { startImport() }
        f.onComplete {
          case scala.util.Success(_) => message("successfull completion")
          case scala.util.Failure(e) =>
            message("error in loading data")
            logger.error("exception for loading data", e)
        }
      case _ => logger.info(s"invalid command for updating resources: $jv")
    }
  }

  override def lowPriority = {
    case jv: JValue => processJson(jv)
  }

  def message(msg: String) = {
    logger.info(msg)
    ngMessage(
      ("cmd" -> "message") ~
      ("data" ->  ("date" -> now) ~ ("message" -> msg)  )
    )
  }

  override def render = super.render.copy(xhtml = Full(this.defaultHtml))
}
