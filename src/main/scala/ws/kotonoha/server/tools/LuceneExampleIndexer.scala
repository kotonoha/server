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

import org.apache.lucene.analysis.gosen.GosenAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.store.SimpleFSDirectory
import java.io.File
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.dictionary.ExampleSentenceRecord
import org.apache.lucene.document.{Field, Document}
import org.apache.lucene.document.Field.{Index, Store}

/**
 * @author eiennohito
 * @since 19.04.12
 */

object LuceneExampleIndexer {
  def main(args: Array[String]) = {
    MongoDbInit.init()
    val path = args(0)

    val ga = new GosenAnalyzer(Version.LUCENE_35)
    val index = new SimpleFSDirectory(new File(path))
    val config = new IndexWriterConfig(Version.LUCENE_35, ga)
    val w = new IndexWriter(index, config)

    docs(d => w.addDocument(d))

    w.close()

  }

  def makeDoc(rec: ExampleSentenceRecord): Document = {
    val doc = new Document
    doc.add(new Field("text", rec.content.is, Store.NO, Index.ANALYZED))
    doc.add(new Field("id", rec.id.toString(), Store.YES, Index.NOT_ANALYZED))
    doc
  }

  def docs(f: Document => Unit) = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    ExampleSentenceRecord where (_.lang eqs "jpn") foreach {r => f(makeDoc(r))}
  }
}
