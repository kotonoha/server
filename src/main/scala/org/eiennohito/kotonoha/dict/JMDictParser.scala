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

import javax.xml.stream.events.XMLEvent
import org.eiennohito.kotonoha.xml._
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import xml.pull.XMLEventReader
import org.eiennohito.kotonoha.records.dictionary._


object JMDictParser {

  import XmlParser._

  def isJmdicNode(x: XmlData) = x match {
    case XmlEl("JMdict") => true
    case _ => false
  }


  import scala.collection.{mutable => mut}

  def parseJmString(it: XmlParseTransformer, name: String, teb: String, tpri: String): JMString = {
    val rec = JMString.createRecord
    it.trans(name) {it =>
      rec.value(it.textOf(teb))
      val pris = it.transSeq(tpri) ( it => it.next() match {
        case XmlText(t) => Some(t)
        case _ => None
      }) filterNot(_.isEmpty) map (_.get)
      rec.priority(pris.map(Priority(_)).toList)
      rec
    }
  }

  def parseSense(it: XmlParseTransformer): JMDictMeaning = {
    it.trans("sense") { it =>
      val rec = JMDictMeaning.createRecord
      val pos = JMDictAnnotations.withName(it.textOf("pos"))
      val mns = it.transSeq("gloss", true) (it => it.next() match {
        case XmlText(t) => t
        case _ => ""
      })
      rec.pos(pos).vals(mns.toList)
    }
  }

  def parse(stream: InputStream) = {
    val fact = XMLInputFactory.newInstance()
    fact.setProperty(XMLInputFactory.IS_VALIDATING, false)
    fact.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)

    val reader = fact.createFilteredReader(fact.createXMLEventReader(stream, "UTF-8"), WhitespaceFilter)
    val parser = XmlParser.parse(reader)
    while (parser.hasNext && !isJmdicNode(parser.next())) {}
    val entries = parser.transSeq("entry") { it =>
      parseEntry(it)
    }
    entries
  }

  def parseEntry(it: XmlParseTransformer): JMDictRecord = {
    val rec = JMDictRecord.createRecord
    val rds = new mut.ListBuffer[JMString]
    val wrs = new mut.ListBuffer[JMString]
    val mns = new mut.ListBuffer[JMDictMeaning]
    rec.id(it.textOf("ent_seq").toLong)
    it.selector {
      case XmlEl("r_ele") => rds += parseJmString(it, "r_ele", "reb", "re_pri")
      case XmlEl("k_ele") => wrs += parseJmString(it, "k_ele", "keb", "ke_pri")
      case XmlEl("sense") => mns += parseSense(it)
    }
    rec.reading(rds.toList).writing(wrs.toList).meaning(mns.toList)
  }
}
