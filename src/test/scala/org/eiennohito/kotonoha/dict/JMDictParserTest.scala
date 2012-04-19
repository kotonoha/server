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

package org.eiennohito.kotonoha.dict

import javax.xml.stream.XMLInputFactory
import org.apache.tools.ant.filters.StringInputStream
import org.eiennohito.kotonoha.xml.{XmlParseTransformer, XmlParser, WhitespaceFilter}
import org.eiennohito.kotonoha.records.dictionary.{JMDictAnnotations, Priority}


class JMDictParserTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  import XmlParser._

  def p(s: String): XmlParseTransformer = {
    val fact = XMLInputFactory.newInstance()
    fact.setProperty(XMLInputFactory.IS_VALIDATING, false)
    fact.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
    val reader = fact.createFilteredReader(fact.createXMLEventReader(new StringInputStream(s, "UTF-8"), "UTF-8"), WhitespaceFilter)
    XmlParser.parse(reader)
  }

  test("jmstring parses successfully") {
    val d = p("""<k_ele>
    <keb>a</keb>
    </k_ele>""")
    val i = JMDictParser.parseJmString(d, "k_ele", "keb", "whatever")
    i.value.get should equal ("a")
  }

  test("jmstring with priority parses successfully") {
    val d = p("""<k_ele>
    <keb>a</keb>
    <ke_pri>ichi2</ke_pri>
    </k_ele>""")
    val i = JMDictParser.parseJmString(d, "k_ele", "keb", "ke_pri")
    i.value.get should equal ("a")
    i.priority.get.head should equal (Priority("ichi2"))
  }

  test("sense parses successfully") {
    val d = p("<sense>\n<pos>&n;</pos>\n<gloss>\"as above\" mark</gloss>\n<gloss xml:lang=\"ger\">(n) siehe oben (Abk.)</gloss>\n</sense>")
    val i = JMDictParser.parseSense(d)
    i.pos.get should equal (JMDictAnnotations.n)
    i.vals.get should have length (2)
  }

  test("one entry parses successfully") {
    val d = p("""<entry>
    <ent_seq>1000050</ent_seq>
    <k_ele>
    <keb>仝</keb>
    </k_ele>
    <r_ele>
    <reb>どうじょう</reb>
    </r_ele>
    <sense>
    <pos>&n;</pos>
    <gloss>"as above" mark</gloss>
    <gloss xml:lang="ger">(n) siehe oben (Abk.)</gloss>
    </sense>
    </entry>""")
    val e = d.trans("entry")(JMDictParser.parseEntry(_))
    val m = e.meaning.is
    m should have length (1)
    val m0 = m(0)
    m0.pos.is should equal (JMDictAnnotations.n)
    m0.vals.is should have size (2)
    e.reading.is.head.value.is should equal ("どうじょう")
  }
}
