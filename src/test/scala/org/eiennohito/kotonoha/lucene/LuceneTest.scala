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

package org.eiennohito.kotonoha.lucene

import org.apache.lucene.analysis.gosen.GosenAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.store.SimpleFSDirectory
import tools.nsc.io.Path
import java.nio.channels.FileChannel
import java.io.{RandomAccessFile, File}
import java.nio.channels.FileChannel.MapMode
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.index.{IndexReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.{TopScoreDocCollector, IndexSearcher}

class LuceneTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {

  test("lucene searches") {
    val dir = new SimpleFSDirectory(new File("e:\\Temp\\lucene_idx\\"))
    val ga = new GosenAnalyzer(Version.LUCENE_35)
    val qp = new QueryParser(Version.LUCENE_35, "text", ga)
    val q = qp.parse("家　寒い")
    val ir = IndexReader.open(dir)
    val is = new IndexSearcher(ir)
    val tsdc = TopScoreDocCollector.create(12, true)
    is.search(q, tsdc)
    val hits = tsdc.topDocs().scoreDocs
    hits.map(h => h -> is.doc(h.doc)).foreach(println(_))
  }

}
