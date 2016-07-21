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

import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.lucene.index.{IndexReader, Term}
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import ws.kotonoha.akane.dic.jmdict.JmdictEntry

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContextExecutor

/**
  * @author eiennohito
  * @since 2016/07/21
  */
case class JmdictQuery(
  limit: Int,
  readings: Seq[JmdictQueryPart],
  writings: Seq[JmdictQueryPart],
  tags: Seq[JmdictQueryPart],
  other: Seq[JmdictQueryPart]
)

case class JmdictQueryPart(term: String, occur: Occur = Occur.SHOULD)


trait LuceneJmdict {
  def find(q: JmdictQuery): Seq[JmdictEntry]
}

class LuceneJmdictImpl(ir: IndexReader, ec: ExecutionContextExecutor) extends LuceneJmdict {

  def makeClause(field: String, jqp: JmdictQueryPart): BooleanClause = {
    val q = new TermQuery(new Term(field, jqp.term))
    new BooleanClause(q, jqp.occur)
  }

  def makeQuery(q: JmdictQuery): Query = {
    val qb = new BooleanQuery.Builder()

    for (r <- q.readings) {
      qb.add(makeClause("r", r))
    }

    for (w <- q.writings) {
      qb.add(makeClause("w", w))
    }

    for (t <- q.tags) {
      qb.add(makeClause("t", t))
    }

    for (o <- q.other) {
      qb.add(makeClause("r", o))
      qb.add(makeClause("w", o))
      qb.add(makeClause("t", o))
      qb.add(makeClause("eng", o))
      qb.add(makeClause("rus", o))
    }

    qb.build()
  }

  private val searcher = new IndexSearcher(ir)

  private val docsCache = {
    Caffeine.newBuilder()
      .executor(ec)
      .maximumSize(20000)
      .expireAfterAccess(1, TimeUnit.DAYS)
      .build[Integer, JmdictEntry]()
  }

  def getDocs(search: TopDocs): Seq[JmdictEntry] = {
    val result = new ArrayBuffer[JmdictEntry](search.scoreDocs.length)

    search.scoreDocs.foreach { sd =>
      val key = Int.box(sd.doc)
      val cached = docsCache.getIfPresent(key)
      if (cached != null) {
        result += cached
      } else {
        val doc = ir.document(sd.doc)
        val blob = doc.getBinaryValue("blob")

        val obj = JmdictEntry.parseFrom(
          new ByteArrayInputStream(
            blob.bytes,
            blob.offset,
            blob.length
          )
        )
        docsCache.put(key, obj)
        result += obj
      }
    }
    result
  }

  override def find(q: JmdictQuery) = {
    val lq = makeQuery(q)
    val search = searcher.search(lq, q.limit)

    getDocs(search)
  }
}
