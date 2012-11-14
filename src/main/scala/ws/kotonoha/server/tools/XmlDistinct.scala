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

import java.io.FileInputStream
import javax.xml.stream.XMLInputFactory
import collection.immutable.HashSet
import ws.kotonoha.akane.xml.{XmlParser, XmlERef, WhitespaceFilter}

/**
 * @author eiennohito
 * @since 14.04.12
 */

object XmlDistinct {
  import resource._

  def listEq(left: List[String], right: List[String]): Boolean = {
    var l = left
    var r = right
    while (l != Nil && r != Nil) {
      val o1 = l.head
      val o2 = r.head
      if (o1 == o2) return true
      l = l.tail
      r = r.tail
    }
    false
  }

  def main(args: Array[String]) = {
    val path = args(0)
    val xpath = args(1)
    val parsed = xpath.split("/").toList.reverse
    for (fl <- managed(new FileInputStream(path))) {
      val fact = XMLInputFactory.newInstance()
      fact.setProperty(XMLInputFactory.IS_VALIDATING, false)
      fact.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
      val reader = fact.createFilteredReader(fact.createXMLEventReader(fl, "UTF-8"), WhitespaceFilter)
      val data = XmlParser.parse(reader).filter(_.isInstanceOf[XmlERef]).filter(t => listEq(t.revpos, parsed)).map(_.data).
        foldLeft(new HashSet[String]){case (set, str) => set + str}
      data.foreach(println(_))
    }
  }

}
