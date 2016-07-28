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
import java.util

import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.ngram.NGramTokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexOptions, IndexWriter}
import org.apache.lucene.util.{BytesRef, BytesRefBuilder}
import org.joda.time.{DateTime, LocalDate}
import ws.kotonoha.akane.dic.jmdict.{JmdictEntry, JmdictTag, JmdictTagMap}

import scala.collection.mutable
import scala.util.hashing.MurmurHash3

/**
  * @author eiennohito
  * @since 2016/07/21
  */
class LuceneImporter(iw: IndexWriter) {

  private val hashSeed = "jmdict".hashCode

  import LuceneImporter._

  def streamTags(entry: JmdictEntry) = {
    val allTags = new mutable.HashSet[JmdictTag]()
    entry.readings.foreach(r => allTags ++= r.info)
    entry.writings.foreach(w => allTags ++= w.info)
    entry.meanings.foreach { m =>
      allTags ++= m.pos
      allTags ++= m.info
    }
    val iter = allTags.iterator.map(t => JmdictTagMap.tagInfo(t.value).repr)
    new StringSeqTokenStream(iter)
  }


  def ref(i: Long) = {
    val bldr = new BytesRefBuilder
    DataConversion.writeSignedVLong(i, bldr)
    bldr.toBytesRef
  }

  def fixedInt(i: Int): BytesRef = {
    val abyte = new Array[Byte](4)
    abyte(0) = ((i >>> 24) & 0xff).toByte
    abyte(1) = ((i >>> 16) & 0xff).toByte
    abyte(2) = ((i >>> 8) & 0xff).toByte
    abyte(3) = ((i >>> 0) & 0xff).toByte
    new BytesRef(abyte)
  }

  def makeDoc(entry: JmdictEntry) = {
    val doc = new Document

    val id = new StringField("id", ref(entry.id), Store.YES)
    doc.add(id)
    val idset = new LongPoint("idset", entry.id)
    doc.add(idset)
    val data = entry.toByteArray
    val blob = new Field("blob", new BytesRef(data), blobField)
    doc.add(blob)

    doc.add(new Field("hash", fixedInt(MurmurHash3.bytesHash(data, hashSeed)), blobField))

    for (r <- entry.readings) {
      doc.add(new StringField("r", r.content, Store.NO))
      val ngrams = new NGramTokenizer(1, 3)
      ngrams.setReader(new StringReader(r.content))
      doc.add(new TextField("rn", ngrams))
    }

    for (w <- entry.writings) {
      doc.add(new StringField("w", w.content, Store.NO))
      val ngrams = new NGramTokenizer(1, 3)
      ngrams.setReader(new StringReader(w.content))
      doc.add(new TextField("wn", ngrams))
    }

    doc.add(new Field("t", streamTags(entry), fullMatchField))

    for {
      m <- entry.meanings
      c <- m.content
    } {
      doc.add(new TextField(c.lang, c.str, Store.NO))
    }
    doc
  }

  def add(entry: JmdictEntry): Unit = {
    val doc = makeDoc(entry)
    iw.addDocument(doc)
  }

  def commit(jmdictDate: LocalDate): Unit = {
    val now = DateTime.now().toString
    val jmdictString = jmdictDate.toString
    val data = new util.HashMap[String, String]()
    data.put(LuceneImporter.INFO_CREATION_DATE, jmdictString)
    data.put(LuceneImporter.INFO_BUILD_DATE, now)
    iw.setCommitData(data)
    iw.commit()
    iw.forceMerge(1, true)
  }
}

object LuceneImporter {
  def parseUserData(userData: util.Map[String, String]) = {
    val creationString = userData.get(INFO_CREATION_DATE)
    val buildString = userData.get(INFO_BUILD_DATE)
    val creation = LocalDate.parse(creationString)
    val build = DateTime.parse(buildString)
    JmdictInfo(creation, build)
  }


  val INFO_CREATION_DATE = "creationDate"
  val INFO_BUILD_DATE = "buildDate"

  val blobField = {
    val tp = new FieldType()
    tp.setStored(true)
    tp.setIndexOptions(IndexOptions.NONE)
    tp.freeze()
    tp
  }

  val fullMatchField = {
    val tp = new FieldType()
    tp.setStored(false)
    tp.setIndexOptions(IndexOptions.DOCS)
    tp.setTokenized(true)
    tp.setOmitNorms(true)
    tp.freeze()
    tp
  }
}

object DataConversion {
  def writeSignedVLong(lng: Long, b: BytesRefBuilder) {
    var i = lng
    while ((i & ~0x7FL) != 0L) {
      b.append(((i & 0x7FL) | 0x80L).toByte)
      i >>>= 7
    }
    b.append(i.toByte)
  }

  def readSignedVLong(b: BytesRef): Long = {
    var l = 0L
    var i = b.offset
    val end = b.length
    while (i < end) {
      l |= (b.bytes(i) & 0x7f)
      i += 1
      if (i < end) {
        l <<= 7
      }
    }
    l
  }
}

class StringSeqTokenStream(input: Iterator[String]) extends TokenStream {

  private[this] val charAttr = addAttribute(classOf[CharTermAttribute])

  override def incrementToken() = {
    if (input.hasNext) {
      charAttr.setEmpty()
      charAttr.append(input.next())
      true
    } else false
  }

}
