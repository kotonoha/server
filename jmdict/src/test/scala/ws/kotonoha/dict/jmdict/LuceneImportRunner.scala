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

package ws.kotonoha.dict.jmdict

import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.InfoStream
import org.slf4j.{Logger, LoggerFactory}
import ws.kotonoha.akane.dic.jmdict.JmdictParser
import ws.kotonoha.akane.resources.Classpath

/**
  * @author eiennohito
  * @since 2016/07/21
  */
object LuceneImportRunner {
  import ws.kotonoha.akane.resources.FSPaths._
  def main(args: Array[String]): Unit = {
    val ipath = args(0).p
    val opath = args(1).p


    val icfg = new IndexWriterConfig(new StandardAnalyzer(new StringReader(Classpath.fileAsString("jmdict-meanings-stopwords.txt"))))
    //icfg.setCodec(new Lucene60Codec())

    icfg.setInfoStream(new InfoStream {
      val loggers = new ConcurrentHashMap[String, Logger]()

      def logger(name: String): Logger = {
        var lg = loggers.get(name)
        if (lg != null) return lg
        lg = LoggerFactory.getLogger(s"lucene.$name")
        loggers.put(name, lg)
        lg
      }

      override def isEnabled(component: String) = logger(component).isDebugEnabled
      override def message(component: String, message: String) = logger(component).debug(message)
      override def close() = {}
    })

    for {
      dir <- new NIOFSDirectory(opath).res
      is <- ipath.inputStream
      iw <- new IndexWriter(dir, icfg).res
    } {
      val rdr = new JmdictParser
      val entries = rdr.parse(is)
      val importer = new LuceneImporter(iw)

      for (e <- entries) {
        importer.add(e)
      }

      iw.flush()
    }
  }
}
