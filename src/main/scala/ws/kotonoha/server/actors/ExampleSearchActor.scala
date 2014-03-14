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

package ws.kotonoha.server.actors

import akka.actor.Actor
import org.apache.lucene.store.{FSDirectory, SimpleFSDirectory}
import java.io.{Closeable, File}
import org.apache.lucene.analysis.gosen.GosenAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.{TopScoreDocCollector, IndexSearcher}
import ws.kotonoha.server.KotonohaConfig
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author eiennohito
 * @since 19.04.12
 */

trait SearchMessage extends KotonohaMessage
case class SearchQuery(query: String, max: Int = 20) extends SearchMessage
case object CloseLucene extends SearchMessage
case object ReloadLucene extends SearchMessage

class Searcher(directory: FSDirectory) extends Closeable {
  val ga = new GosenAnalyzer(Version.LUCENE_35)
  val parser = new QueryParser(Version.LUCENE_35, "text", ga)
  val ir = IndexReader.open(directory)
  val searcher = new IndexSearcher(ir)

  def topIds(q: String, max: Int) = {
    val query = parser.parse(q)
    val collector = TopScoreDocCollector.create(max, true)
    searcher.search(query, collector)
    val docs = collector.topDocs()
    docs.scoreDocs.map(d => searcher.doc(d.doc).get("id").toLong).toList
  }

  def close() {
    ir.close()
    directory.close()
  }
}

class ExampleSearchActor extends Actor with Logging {

  var searcher = createSearcher

  def createSearcher: Option[Searcher] = {
    val dirname = KotonohaConfig.safeString("lucene.indexdir")
    val result = dirname.map(new File(_)).filter(_.exists()).map(s => new Searcher(new SimpleFSDirectory(s)))
    if (result.isEmpty)
      logger.warn("Can not search for examples, lucene.indexdir is not configured, will do nothing")
    result
  }

  def findDocs(q: String, max: Int): List[Long] = {
    searcher match {
      case None => Nil
      case Some(s) => s.topIds(q, max)
    }
  }

  override def receive = {
    case SearchQuery(q, max) => {
      val docs = if (q.length == 0) Nil else findDocs(q, max)
      sender ! docs
    }
    case CloseLucene =>
      searcher.map(_.close())
      searcher = None
    case ReloadLucene =>
      searcher.map(_.close())
      searcher = createSearcher
  }

  override def postStop() {
    searcher.foreach(_.close())
  }
}
