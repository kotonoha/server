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

import scalax.io.Input
import scalax.file.Path
import ws.kotonoha.server.mongodb.MongoDbInit
import ws.kotonoha.server.util.unapply.XLong
import ws.kotonoha.server.records.dictionary.{ExampleLinkRecord, ExampleSentenceRecord}
import java.nio.ByteBuffer
import ws.kotonoha.server.dict.TatoebaLink
import java.io.{FileOutputStream, RandomAccessFile}

/**
 * @author eiennohito
 * @since 19.04.12
 */

object TatoebaLinkParser {

  def parseCodes(in: Input) = {
    in.lines(includeTerminator = false).
      map(_.split("\t")).collect {
      case Array(XLong(id), lang, _) => id -> lang
    }.toMap
  }


  def main(args: Array[String]) = {
    val fl = Path.fromString(args(0))
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

/**
 * This importer adds tatoeba links to database
 *
 * I don't really use this one.
 */
object TatoebaImporter {

  def loadLinks(links: Input) = {
    var i = 0L
    for (line <- links.lines(includeTerminator = false)) {
      val x: Any = line.split("\t") match {
        case Array(XLong(left), XLong(right)) => {
          val rec = ExampleLinkRecord.createRecord
          rec.left(left).right(right).
            leftLang(ExampleSentenceRecord.langOf(left)).
            rightLang(ExampleSentenceRecord.langOf(right)).id(i)
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
    val path = Path.fromString(dir)
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
    grps.transform {
      case (k, l) => l.map(_._2).toList
    }
  }

  def load(in: Input, tags: Map[Long, List[String]]) {
    for (line <- in.lines(includeTerminator = false)) {
      val x: Any = line.split("\t") match {
        case Array(XLong(id), lang, sent) => {
          val tag = tags.get(id).getOrElse(Nil)
          val s = ExampleSentenceRecord.createRecord.
            tags(tag).lang(lang).content(sent).id(id)
          s.save
        }
        case _ => println("line wasn't processed:" + line)
      }
    }
  }

  def main(args: Array[String]) = {
    MongoDbInit.init()
    val dir = args(0)
    val path = Path.fromString(dir)
    val tags = path / "tags.csv"
    val map = loadTags(tags)
    load(path / "sentences.csv", map)
  }
}
