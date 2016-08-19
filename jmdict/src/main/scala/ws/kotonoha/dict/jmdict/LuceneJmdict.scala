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
import org.apache.commons.lang3.StringUtils
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.{IndexReader, Term}
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search._
import ws.kotonoha.akane.dic.jmdict.JmdictEntry

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContextExecutor
import org.apache.lucene.util.automaton.TooComplexToDeterminizeException
import org.joda.time.{DateTime, LocalDate}

/**
  * @author eiennohito
  * @since 2016/07/21
  */
case class JmdictSearchResults(
  data: Seq[JmdictEntry],
  expls: Map[Long, Explanation] = Map.empty,
  totalHits: Int = 0
)

case class JmdictInfo(update: LocalDate, build: DateTime)


trait LuceneJmdict {
  def find(q: JmdictQuery): JmdictSearchResults
  def ids(q: JmdictIdQuery): Seq[Long]
  def byId(id: Long): Option[JmdictEntry]
  def info: JmdictInfo
}

class LuceneJmdictImpl(ir: IndexReader, ec: ExecutionContextExecutor, val info: JmdictInfo) extends LuceneJmdict {

  val specialChars = "?*.？＊。．"

  def rewriteForLucene(s: String) = {
    s.map {
      case '.' | '。' | '．' | '？' => '?'
      case '＊' => '*'
      case x => x
    }
  }

  def makeLanguageQuery(field: String, termString: String) = {
    val toLucene = rewriteForLucene(termString)

    try {
      val q = new WildcardQuery(new Term(field, toLucene))
      q.rewrite(ir)
    } catch {
      case x if x.isInstanceOf[BooleanQuery.TooManyClauses] || x.isInstanceOf[TooComplexToDeterminizeException] =>
        new TermQuery(new Term(field, StringUtils.removePattern(toLucene, "[\\?\\*]")))
    }

  }

  def makeNgramQuery(field: String, termString: String) = {
    val rewritten = rewriteForLucene(termString)
    val stars = rewritten.count(_ == '*')
    val questions = rewritten.count(_ == '?')
    (stars, questions) match {
      //case (1, 0) =>
      //case (0, 1) =>
      //case (0, n) =>
      case (x, y) => makeLanguageQuery(field, rewritten)
    }
  }

  def parseSpecialQuery(field: String, termString: String): Query = {
    field match {
      case "w" | "r" => makeNgramQuery(field, termString)
      case s if s.length == 3 => makeLanguageQuery(field, termString)
      case _ => new TermQuery(new Term(field, termString))
    }
  }

  def makeClause(field: String, jqp: JmdictQueryPart, boost: Float = 1.0f): BooleanClause = {
    val termString = jqp.term
    val q = if (StringUtils.containsAny(termString, specialChars)) {
      parseSpecialQuery(field, termString)
    } else {
      new TermQuery(new Term(field, jqp.term))
    }

    val wrapped = q match {
      case x: ConstantScoreQuery => x
      case _ => new ConstantScoreQuery(q)
    }

    val boosted = if (boost == 1.0) wrapped else {
      new BoostQuery(wrapped, boost)
    }

    new BooleanClause(boosted, jqp.occur)
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
      qb.add(makeClause("t", t, 0.9f))
    }

    if (q.ignore.nonEmpty) {
      val qx = LongPoint.newSetQuery("idset", q.ignore :_*)
      qb.add(qx, Occur.MUST_NOT)
    }

    for (o <- q.other) {
      val iq = new BooleanQuery.Builder
      iq.setDisableCoord(true)
      val cc = o.copy(occur = Occur.SHOULD)
      iq.add(makeClause("r", cc, 0.95f))
      iq.add(makeClause("w", cc))
      iq.add(makeClause("t", cc, 0.9f))
      iq.add(makeClause("eng", cc, 0.7f))
      iq.add(makeClause("rus", cc, 0.7f))
      o.occur match {
        case Occur.MUST_NOT | Occur.MUST =>
          iq.setMinimumNumberShouldMatch(1)
        case _ =>
      }
      qb.add(iq.build(), o.occur)
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

  def getDocs(search: TopDocs, q: Query, explain: Boolean) = {
    val result = new ArrayBuffer[JmdictEntry](search.scoreDocs.length)
    var expls = Map[Long, Explanation]()

    search.scoreDocs.foreach { sd =>
      val key = Int.box(sd.doc)
      var cached = docsCache.getIfPresent(key)
      if (cached == null) {
        val doc = ir.document(sd.doc)
        val blob = doc.getBinaryValue("blob")

        cached = JmdictEntry.parseFrom(
          new ByteArrayInputStream(
            blob.bytes,
            blob.offset,
            blob.length
          )
        )
        docsCache.put(key, cached)
      }
      result += cached

      if (explain) {
        val explanation = searcher.explain(q, sd.doc)
        expls += (cached.id -> explanation)
      }
    }
    JmdictSearchResults(result, expls, search.totalHits)
  }

  override def find(q: JmdictQuery): JmdictSearchResults = {
    val lq = makeQuery(q)
    val search = searcher.search(lq, q.limit)

    getDocs(search, lq, q.explain)
  }

  import scala.collection.JavaConverters._

  def makeIdQuery(parts: Seq[IdQueryPart]): Query = {
    val bq = new BooleanQuery.Builder
    bq.setDisableCoord(true)

    parts.foreach { p =>
      val inner = new BooleanQuery.Builder
      p.rds.foreach { r =>
        inner.add(new TermQuery(new Term("r", r)), Occur.MUST)
      }
      p.wrs.foreach { w =>
        inner.add(new TermQuery(new Term("w", w)), Occur.MUST)
      }
      bq.add(inner.build(), Occur.SHOULD)
    }

    bq.build()
  }

  override def byId(id: Long) = {
    val q = new TermQuery(new Term("id", DataConversion.longBytes(id)))
    val res = searcher.search(q, 1)
    getDocs(res, q, explain = false).data.headOption
  }

  override def ids(q: JmdictIdQuery): Seq[Long] = {

    val onlyId = Set("id").asJava
    val ids = new ArrayBuffer[Long]()
    q.parts.grouped(500).foreach { qp =>
      val lq = makeIdQuery(qp)
      searcher.search(lq, new SimpleCollector {
        override def collect(doc: Int) = {
          val binary = searcher.doc(doc, onlyId).getBinaryValue("id")
          ids += DataConversion.readSignedVLong(binary)
        }
        override def needsScores() = false
      })
    }

    ids
  }
}
