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

package org.eiennohito.kotonoha

import net.liftweb.util.Html5
import xml.{Elem, NodeSeq, Node}
import java.io.{PrintWriter, FileInputStream}

/**
 * @author eiennohito
 * @since 02.03.12
 */


class SomeTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  test("someTest") {
    val is = new FileInputStream("""e:\Temp\kanji\joyou_wiki.htm""")
    val el = Html5.parse(is)
    val e = el.openTheBox
    val nodes = e \\ "tbody" \ "tr"
    val kanji = nodes.collect {
      case x : Elem => (x \\ "td")(1).text
    }
    is.close()

    val out = new PrintWriter("""e:/temp/kanji/java.txt""", "utf-8")

    kanji.foreach { k =>
      val s = "\"%s\",".format(k)
      out.println(s)
    }
    out.close()
  }
}