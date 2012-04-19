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
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import tools.nsc.io.Path
import java.nio.channels.FileChannel
import java.io.{RandomAccessFile, File}
import java.nio.channels.FileChannel.MapMode

class LuceneTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  implicit def indexWriter2docAdder(i: IndexWriter) = new DocAdder(i)

  test("smt") {
    val ga = new GosenAnalyzer(Version.LUCENE_35)
    val index = new SimpleFSDirectory(new File("e:/Temp/lucene_idx"))
    val config = new IndexWriterConfig(Version.LUCENE_35, ga)
    val w = new IndexWriter(index, config)


  }

  class DocAdder(i: IndexWriter) {

  }


  test("channels and buffers") {
    val fl = new File("e:/temp/asd")
    val raf = new RandomAccessFile(fl, "rw")
    val fc = raf.getChannel
    val buf = fc.map(MapMode.READ_WRITE, 0, 4096)
    val lb = buf.asLongBuffer()
    val al = (0L until 512L).toArray
    lb.put(al)
    buf.force()
    fc.force(false)
    fc.close()
  }

}
