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

package ws.kotonoha.server.web.comet

import java.net._
import java.util.zip.{GZIPInputStream, ZipFile}

import com.google.inject.Inject
import com.typesafe.config.Config
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.common.Full
import net.liftweb.http.CometActor
import net.liftweb.json.JsonAST.{JString, JValue}
import org.apache.commons.io.IOUtils
import resource.Resource
import ws.kotonoha.server.KotonohaConfig
import ws.kotonoha.server.actors.dict.{CloseExampleIndex, LoadExampleIndex}
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import ws.kotonoha.server.actors.lift.{AkkaInterop, NgLiftActor}
import ws.kotonoha.server.actors.{CloseLucene, ReloadLucene, TellAllUsers}
import ws.kotonoha.server.dict.JmdictService
import ws.kotonoha.server.tools._

import scala.concurrent.Future
import scalax.file.Path
import scalax.io.StandardOpenOption

/**
 * @author eiennohito
 * @since 2014-03-13
 */
class UpdateResourcesActor @Inject() (
  jms: JmdictService,
  cfg: Config
) extends CometActor with NgLiftActor with AkkaInterop with Logging with ReleaseAkka {

  import Resource._
  import ws.kotonoha.server.util.DateTimeUtils.now
  import ws.kotonoha.server.util.KBsonDSL._

  val self = this

  override def receiveJson = {
    case x => self ! x
  }

  override def svcName = "UpdateResourcesSvc"

  def download(url: URL, path: Path) = {
    message(s"downloading $url to $path")
    val conn = KotonohaConfig.safeString("dl.http.proxy.addr") match {
      case Some(addr) =>
        val proxyConfig = new Proxy(
          Proxy.Type.HTTP,
          new InetSocketAddress(addr, KotonohaConfig.safeInt("dl.http.proxy.port").getOrElse(8080))
        )
        url.openConnection(proxyConfig)
      case _ => url.openConnection()
    }
    for (ifs <- resource.managed(conn.getInputStream);
         ofs <- path.outputStream(StandardOpenOption.Create)) {
      IOUtils.copy(ifs, ofs)
      message(s"downloading $url succeeded")
    }
  }

  def processTatoeba(dir: Path, sentsUrl: String, linksUrl: String, tagsUrl: String) = {
    toAkka(CloseLucene)
    toAkka(TellAllUsers(CloseLucene))
    toAkka(CloseExampleIndex)
    toAkka(TellAllUsers(CloseExampleIndex))

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
    toAkka(TellAllUsers(ReloadLucene))

    KotonohaConfig.safeString("example.index") match {
      case None => message("ERROR: example.index is not configured!")
      case Some(ei) =>
        message("creating example link index")
        TatoebaLinkParser.produceExampleLinks(links, sentences, Path.fromString(ei))
    }

    toAkka(LoadExampleIndex)
    toAkka(TellAllUsers(LoadExampleIndex))
  }

  def processKanjidic(dir: Path) = {
    val url = cfg.getString("resources.external.kanjidic")

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
    val url = cfg.getString("resources.external.warodai")

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
    //update jmdict
    jms.maybeUpdateJmdict()

    message("starting processing:")
    message(s"lucene path: ${KotonohaConfig.safeString("lucene.indexdir")}")
    message(s"example index dir: ${KotonohaConfig.safeString("example.index")}")

    val dir = Path.createTempDirectory(prefix = "kotonoha", suffix = "import")

    message(s"using $dir as temporary directory")


    processTatoeba(dir,
      cfg.getString("resources.external.tatoeba.sentences"),
      cfg.getString("resources.external.tatoeba.links"),
      cfg.getString("resources.external.tatoeba.tags"))

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
    ngMessageRaw(
      ("cmd" -> "message") ~
      ("data" ->  ("date" -> now) ~ ("message" -> msg)  )
    )
  }

  override def render = super.render.copy(xhtml = Full(this.defaultHtml))
}
