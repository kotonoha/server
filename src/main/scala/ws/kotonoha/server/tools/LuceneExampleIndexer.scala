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

import java.nio.file.Paths

import org.apache.lucene.analysis.gosen.GosenAnalyzer
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.{Document, StringField, TextField}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.SimpleFSDirectory
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.records.dictionary.ExampleSentenceRecord

/**
 * @author eiennohito
 * @since 19.04.12
 */

object LuceneExampleIndexer {
  def main(args: Array[String]) = {
    MongoDbInit.init()
    val path = args(0)

    createIndex(path)
  }


  def createIndex(path: String) {
    for {
      ga <- resource.managed(new GosenAnalyzer())
      index <- resource.managed(new SimpleFSDirectory(Paths.get(path)))
      config = new IndexWriterConfig(ga)
      w <- resource.managed(new IndexWriter(index, config))
    } {
      w.deleteAll()
      docs(d => w.addDocument(d))
    }
  }

  def makeDoc(rec: ExampleSentenceRecord): Document = {
    val doc = new Document

    doc.add(new TextField("text", rec.content.get, Store.NO))
    doc.add(new StringField("id", rec.id.toString(), Store.YES))
    doc
  }

  def docs(f: Document => Unit) = {
    import ws.kotonoha.server.mongodb.KotonohaLiftRogue._
    ExampleSentenceRecord where (_.lang eqs "jpn") foreach {r => f(makeDoc(r))}
  }
}
