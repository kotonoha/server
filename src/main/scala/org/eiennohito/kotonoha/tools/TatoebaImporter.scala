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

package org.eiennohito.kotonoha.tools

import scalax.io.Input
import scalax.file.Path
import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.util.unapply.XLong
import org.eiennohito.kotonoha.records.dictionary.{ExampleLinkRecord, ExampleSentenceRecord}
import java.nio.ByteBuffer
import org.eiennohito.kotonoha.dict.TatoebaLink
import java.io.{FileOutputStream, RandomAccessFile}

/**
 * @author eiennohito
 * @since 19.04.12
 */

object TatoebaLinkParser {

  def parseCodes(in: Input) = {
    in.lines(includeTerminator = false).
      map(_.split("\t")).collect{
      case Array(XLong(id), lang, _) => id -> lang
    }.toMap
  }


  def main(args: Array[String]) = {
    val fl = Path(args(0))
    val links = fl / "links.csv"
    val bin = fl / "links.bin"
    val map = parseCodes(fl / "sentences.csv")
    val rf = new FileOutputStream(bin.path, false)
    val chn = rf.getChannel
    val buf = ByteBuffer.allocate(8 * 4)
    for (line <- links.lines(includeTerminator = false)) {
      val x: Any = line.split("\t") match {
        case Array(XLong(left), XLong(right)) => {
          val o = TatoebaLink(left, right,
            map.get(left).getOrElse("non"),
            map.get(right).getOrElse("non"))
          buf.clear()
          o.toBuffer(buf)
          buf.flip()
          chn.write(buf)
        }
      }
    }
    chn.force(false)
    chn.close()
    rf.flush()
    rf.close()
  }
}

object TatoebaImporter {

  def loadLinks(links: Input) = {
    var i = 0L
    for (line <- links.lines(includeTerminator = false)) {
      val x : Any = line.split("\t") match {
        case Array(XLong(left), XLong(right)) => {
          val rec = ExampleLinkRecord.createRecord
          rec.left(left).right(right).id(i).
            leftLang(ExampleSentenceRecord.langOf(left)).
            rightLang(ExampleSentenceRecord.langOf(right))
          i += 1
          rec.save
        }
        case _ => println("line wasn't processed:" + line)
      }
    }
  }

  def main(args: Array[String]) = {
      MongoDbInit.init()
      val dir = args(0)
      val path = Path(dir)
      val links = path / "links.csv"
      loadLinks(links)
    }
}

object TatoebaSentenceImporter {
  def loadTags(in: Input) = {
    val grps = in.lines(includeTerminator = false).map {
      s =>
        val arr = s.split("\t")
        arr(0).toLong -> arr(1)
    }.groupBy(_._1)
    grps.transform{case (k, l) => l.map(_._2).toList}
  }

  def load(in: Input, tags: Map[Long, List[String]]) {
    for (line <- in.lines(includeTerminator = false)) {
      val x : Any = line.split("\t") match {
        case Array(XLong(id), lang, sent) => {
          val tag = tags.get(id).getOrElse(Nil)
          val s = ExampleSentenceRecord.createRecord.id(id).
            tags(tag).lang(lang).content(sent)
          s.save
        }
        case _ => println("line wasn't processed:" + line)
      }
    }
  }

  def main(args: Array[String]) = {
    MongoDbInit.init()
    val dir = args(0)
    val path = Path(dir)
    val tags = path / "tags.csv"
    val map = loadTags(tags)
    load(path / "sentences.csv", map)
  }
}
