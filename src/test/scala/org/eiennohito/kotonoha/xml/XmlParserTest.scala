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

package org.eiennohito.kotonoha.xml

import javax.xml.stream.XMLInputFactory
import org.apache.tools.ant.filters.StringInputStream


class XmlParserTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  def p(s: String) = {
    val fact = XMLInputFactory.newInstance()
    fact.setProperty(XMLInputFactory.IS_VALIDATING, false)
    fact.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
    val reader = fact.createFilteredReader(fact.createXMLEventReader(new StringInputStream(s, "UTF-8"), "UTF-8"), WhitespaceFilter)
    XmlParser.parse(reader)
  }

  test("xmlparser state works") {
    val s = new ParserState
    s.push("test")
    s.top should equal (List("test"))
    s.pop()
    evaluating { s.top } should produce[NoSuchElementException]
  }

  test("xmlparser matches successfully") {
    val s = p("<help />")
    val parsed = s.toList
    val head = parsed.head
    head.data should equal ("help")
    head match {
      case XmlEl(t) => t should equal ("help")
      case x @ _ => fail("Something invalid matched:" + x)
    }
  }


  test("xmlparser parses double tag") {
    val xml = p("""<div>
      <span>hey!</span>
    </div>""")
    val parsed = xml.toArray
    parsed should have length (5)
    parsed(2) should have (
      'pos ("div" :: "span" :: Nil),
      'data ("hey!")
    )
  }

  import XmlParser._

  test("textOf works") {
    val xml = p("<a>hell</a>")
    xml.textOf("a") should equal ("hell")
  }

  test("nested textof works") {
    val xml = p("<b><a>txt</a></b>")
    xml.trans("b")(_.textOf("a")) should equal ("txt")
  }

  test("parser with selector works") {
    val xml: XmlParseTransformer = p("<b>text</b>")
    xml.selector {
      case XmlEl("b") => xml.textOf("b") should equal ("text")
      case _ => fail("shouldn't get here")
    }
  }

  test("2 tags works") {
    val xml = p("<k><a>asd</a><b>sdaf</b></k>")
    xml.trans("k") { xml =>
      val f = xml.trans("a")(_.next().data)
      val g = xml.trans("b")(_.next().data)
      f should equal ("asd")
      g should equal ("sdaf")
    }
  }
}
