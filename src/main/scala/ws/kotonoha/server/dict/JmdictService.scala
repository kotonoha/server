/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.dict

import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.function.Function

import akka.actor.Scheduler
import com.github.benmanes.caffeine.cache.{Caffeine, RemovalCause, RemovalListener}
import com.google.inject.{Inject, Provider}
import com.typesafe.config.Config
import org.apache.lucene.analysis.core.SimpleAnalyzer
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.store._
import org.joda.time.{DateTime, LocalDate}
import ws.kotonoha.akane.dic.jmdict.{JMDictUtil, JmdictParser}
import ws.kotonoha.akane.resources.FSPaths
import ws.kotonoha.dict.jmdict._
import ws.kotonoha.server.ioc.Res
import ws.kotonoha.server.util.Downloads

import scala.concurrent.ExecutionContextExecutor

/**
  * @author eiennohito
  * @since 2016/07/27
  */

trait JmdictService extends Provider[LuceneJmdict] {
  def versions: Seq[String]
  def indexOf(version: String): LuceneJmdict
  def maybeUpdateJmdict(): Unit
}

case class JmdictInstance (
  dir: Directory,
  reader: DirectoryReader,
  info: JmdictInfo,
  instance: LuceneJmdict
)

class JmdictServiceImpl @Inject() (
  cfg: Config,
  ece: ExecutionContextExecutor,
  sched: Scheduler,
  res: Res
) extends JmdictService {

  import ws.kotonoha.akane.resources.FSPaths._

  val jmdictRoot = cfg.getString("jmdict.path").p

  val indices = jmdictRoot / "indices"
  val cache = jmdictRoot / "cache"
  val temp = jmdictRoot / "temp"

  indices.mkdirs()
  cache.mkdirs()
  temp.mkdirs()

  import ws.kotonoha.server.util.DateTimeUtils._

  import scala.concurrent.duration._

  private val canceller = sched.schedule(1.minute, 1.day) {
    maybeUpdateJmdict()
  } (ece)

  res.register(new AutoCloseable {
    override def close() = canceller.cancel()
  })

  private def isLuceneDir(p: Path): Boolean = {
    (p / "write.lock").exists
  }

  @volatile
  private[this] var cachedVersions: List[String] = {
    val finder = FSPaths.find(indices, 1) { case (p, a) => a.isDirectory && isLuceneDir(p) }
    val x = finder.map { y =>
      val objs = y.map(_.getFileName.toString).toList
      objs.sorted(implicitly[Ordering[String]].reverse)
    }
    x.obj
  }

  override def versions = cachedVersions

  private[this] val instanceCache = {
    Caffeine.newBuilder()
      .executor(ece)
      .expireAfterAccess(48, TimeUnit.HOURS)
      .removalListener(new RemovalListener[String, JmdictInstance] {
        override def onRemoval(key: String, value: JmdictInstance, cause: RemovalCause) = {
          res.closeResources(value.reader, value.dir)
        }
      })
      .build[String, JmdictInstance]()
  }

  private[this] val maker = new Function[String, JmdictInstance] {
    override def apply(directoryName: String) = {
      val path = indices / directoryName
      val baseDir = res.makeOrCached(path, new MMapDirectory(path))
      val dreader = DirectoryReader.open(baseDir)
      res.register(dreader)
      val info = LuceneImporter.parseUserData(dreader.getIndexCommit.getUserData)
      val impl = new LuceneJmdictImpl(dreader, ece, info)
      JmdictInstance(baseDir, dreader, info, impl)
    }
  }

  override def indexOf(version: String) = instanceCache.get(version, maker).instance

  override def get() = {
    val vers = versions
    if (vers.isEmpty) EmptyJmdict else indexOf(vers.head)
  }

  def downloadJmdict(): Path = {
    val uri = new URI(cfg.getString("resources.external.jmdict"))
    val now = DateTime.now()
    val tmpFile = temp / s"Jmdict.$now.gz"
    Downloads.download(uri, tmpFile)
    val updDate = {
      val date = JMDictUtil.extractVersion(tmpFile).map(LocalDate.parse)
      date.getOrElse(now.toLocalDate)
    }
    val cached = cache / s"JMDict.$updDate.gz"
    cached.deleteIfExists()
    tmpFile.moveTo(cached)
    cached
  }

  def maybeDownloadJmdict(): Path = {
    val finder = FSPaths.find(cache, 1) { case (p, a) => a.isRegularFile && p.getFileName.toString.startsWith("JMDict.") }
    val data = finder.map(_.map { p =>
      val name = p.getFileName.toString
      val date = name.substring(7, name.length - 3)
      LocalDate.parse(date) -> p
    }.toVector.sortWith { case ((l1, _), (l2, _)) => l1.compareTo(l2) > 0}).obj

    val mostRecent = data.headOption
    mostRecent match {
      case None => //no JMdict exists
        downloadJmdict()
      case Some((ld, p)) =>
        val now = DateTime.now
        if (ld.toDateTimeAtCurrentTime.plus(14.days).isBefore(now)) {
          downloadJmdict()
        } else p
    }
  }

  def updateJmdict(file: Path) = {
    val date = DateTime.now()
    val outName = date.toString()
    val tmpDir = temp / outName

    val iwc = new IndexWriterConfig(new SimpleAnalyzer)
    val mkDate = JMDictUtil.extractVersion(file).map(LocalDate.parse).getOrElse(date.toLocalDate)

    for {
      dir <- new NIOFSDirectory(tmpDir).res
      iw <- new IndexWriter(dir, iwc).res
      is <- file.inputStream
    } {
      val importer = new LuceneImporter(iw)
      val reader = new JmdictParser
      reader.parse(JMDictUtil.convertStream(file.extension, is)).foreach(e => importer.add(e))
      importer.commit(mkDate)
    }

    tmpDir.moveTo(indices / outName)

    cachedVersions = outName :: cachedVersions
  }

  def maybeUpdateJmdict(): Unit = {
    val now = DateTime.now()
    versions.headOption.map(DateTime.parse) match {
      case None => updateJmdict(maybeDownloadJmdict())
      case Some(dt) =>
        if (dt.plusDays(4).isBefore(now)) {
          val path = maybeDownloadJmdict()
          val date = JMDictUtil.extractVersion(path).map(LocalDate.parse).getOrElse(now.toLocalDate)
          if (date.toDateTimeAtStartOfDay.isAfter(dt)) {
            updateJmdict(path)
          }
        }

    }
  }

}

object EmptyJmdict extends LuceneJmdict {
  override def find(q: JmdictQuery) = JmdictSearchResults.apply(
    Nil, Map.empty, 0
  )
  override val info = JmdictInfo.apply(LocalDate.now(), DateTime.now().minusYears(5))
  override def ids(q: JmdictIdQuery) = Nil
}
