package ws.kotonoha.server.kanji

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.io.{PrintWriter, FileInputStream, InputStreamReader, ByteArrayInputStream}
import ws.kotonoha.akane.kanji.KanjiTagger

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
/**
 * @author eiennohito
 * @since 02.03.12
 */

class GrouperTest extends FunSuite with ShouldMatchers {
  test("correct tagging") {
    val s = "この人間の人生は駄目でした"
    val inp = new InputStreamReader(new ByteArrayInputStream(s.getBytes("utf8")), "utf8")
    val t = new KanjiTagger
    val res = t.tag(inp)
    println(res.oldKanji)
  }

  ignore("tag bake") {
    val path = "e:\\books\\no-tech\\japanese\\bakemonogatari\\[20070627] Bakemonogatari - 1\\text.txt"
    val inp = new FileInputStream(path)
    val r = new InputStreamReader(inp, "utf8")
    val t = new KanjiTagger
    val res = t.tag(r)
    val total = res.total.sortWith {
      case (k1, k2) => k1.count < k2.count
    }.toArray

    val out = new PrintWriter("e:/temp/kanji/bake.txt", "utf8")
    out.println("Total: %d kanji in bake".format(total.length))
    out.println("%d old, %d new, %d removed, %d absent".format(res.oldKanji.length, res.newKanji.length, res.removedKanji.length, res.absentKanji.length))
    total.foreach {k =>
      out.println("%s: %d, %s".format(k.kanji, k.count, k.category))
    }
    out.close()
    r.close()
  }
}
