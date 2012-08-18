package ws.kotonoha.server.xml

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

import xml.pull.XMLEventReader
import io.Source
import java.io.{InputStream, FileInputStream, File}
import javax.xml.stream.{EventFilter, XMLInputFactory}
import javax.xml.stream.events._
import java.util.regex.Pattern

/**
 * @author eiennohito
 * @since 13.04.12
 */

object Chars {
  def unapply(x: XMLEvent) = x match {
    case n: Characters => Some(n.getData)
    case _ => None
  }
}

object StElem {
  def unapply(x: XMLEvent): Option[String] = x match {
    case n: StartElement => Some(n.getName.getLocalPart)
    case _ => None
  }
}

object EnElem {
  def unapply(x: XMLEvent): Option[String] = x match {
    case n: EndElement => Some(n.getName.getLocalPart)
    case _ => None
  }
}


object WhitespaceFilter extends EventFilter {
  val whitespace = Pattern.compile("^[\\s+]+$", Pattern.MULTILINE)

  def accept(event: XMLEvent) = event match {
    case Chars(s) => !whitespace.matcher(s).matches()
    case _ => true
  }
}


