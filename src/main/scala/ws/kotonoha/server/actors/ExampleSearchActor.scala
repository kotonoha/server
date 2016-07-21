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

import java.io.{Closeable, File}

import akka.actor.Actor
import com.typesafe.scalalogging.{StrictLogging => Logging}
import org.apache.lucene.store.FSDirectory
import ws.kotonoha.server.KotonohaConfig

/**
 * @author eiennohito
 * @since 19.04.12
 */

trait SearchMessage extends KotonohaMessage
case class SearchQuery(query: String, max: Int = 20) extends SearchMessage
case object CloseLucene extends SearchMessage
case object ReloadLucene extends SearchMessage

class Searcher(directory: FSDirectory) extends Closeable {
  def topIds(q: String, max: Int) = { Nil }

  def close() {
    directory.close()
  }
}

class ExampleSearchActor extends Actor with Logging {

  var searcher = createSearcher

  def createSearcher: Option[Searcher] = {
    val dirname = KotonohaConfig.safeString("lucene.indexdir")
    val result = dirname.map(new File(_)).filter(_.exists()).map(s => new Searcher(null))
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
