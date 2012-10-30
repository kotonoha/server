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

package ws.kotonoha.server.xml

import javax.xml.stream.XMLEventReader
import collection.mutable.Stack
import javax.xml.stream.events._
import scala.None
import ws.kotonoha.akane.utils.CalculatingIterator


trait XmlData {
  def data: String

  private var where: List[String] = Nil

  private[xml] def at(pos: List[String]): XmlData = {
    where = pos
    this
  }

  lazy val pos = where.reverse

  def revpos = where
}

case class XmlText(data: String) extends XmlData

case class XmlERef(data: String) extends XmlData

case class XmlEl(data: String) extends XmlData {
  var attrs = Map[String, String]()

  def this(data: String, attrmap: Map[String, String]) = {
    this(data)
    attrs = attrmap
  }

  def apply(key: String) = attrs(key)
}

case class XmlElEnd(data: String) extends XmlData

private[xml] class ParserState {

  def push(name: String) = {
    if (stack.isEmpty) {
      stack.push(List(name))
    } else {
      val t = top
      stack.push(name :: t)
    }
  }

  def pop() = stack.pop()

  val stack = Stack[List[String]]()

  def top = stack.top
}

class XmlIterator(in: XMLEventReader) extends CalculatingIterator[XmlData] {
  private lazy val state: ParserState = new ParserState

  private def extractAttributeMap(st: StartElement): Map[String, String] = {
    import scala.collection.JavaConversions._
    val iter = st.getAttributes
    if (!iter.hasNext) {
      return Map[String, String]()
    }
    val x = iter.map(_.asInstanceOf[Attribute]).map(a => a.getName.getLocalPart -> a.getValue)
    x.toMap
  }

  private def transform(next: XMLEvent): Option[XmlData] = {
    next match {
      case st: StartElement => {
        val map = extractAttributeMap(st)
        val name = st.getName.getLocalPart
        state.push(name)
        Some(new XmlEl(name, map) at (state.top))
      }
      case en: EndElement => {
        val name = en.getName.getLocalPart
        val path = state.pop() // TODO:check equality
        Some(XmlElEnd(name) at (path))
      }
      case t: Characters => Some(XmlText(t.getData) at (state.top))
      case er: EntityReference => Some(XmlERef(er.getName) at (state.top))
      case _ => None
    }
  }

  protected def calculate(): Option[XmlData] = {
    var ok = true
    while (ok && in.hasNext) {
      val next = in.nextEvent()
      val ev = transform(next)
      ok = ev.isEmpty
      if (!ok) {
        return ev
      }
    }
    None
  }
}

class XmlParseTransformer(in: CalculatingIterator[XmlData]) {

  def next() = in.next()

  def selector[T](pf: PartialFunction[XmlData, T]) = {
    while (in.hasNext) {
      val n = in.head
      if (pf.isDefinedAt(n)) {
        pf.apply(n)
      } else {
        in.next()
      }
      //if (in.hasNext) in.next()
    }
  }

  def transSeq[T](name: String)(processor: XmlParseTransformer => T): Iterator[T] = new CalculatingIterator[T] {
    protected def calculate(): Option[T] = {
      val work = true
      while (work && in.hasNext) {
        val n = in.next()
        n match {
          case XmlEl(nm) if nm == name => {
            return Some(processor(untilEndTag(name)))
          }
          case _ => //do nothing
        }
      }
      None
    }
  }

  def transSeq[T](name: String, noattr: Boolean)(processor: XmlParseTransformer => T): Iterator[T] = new CalculatingIterator[T] {
    protected def calculate(): Option[T] = {
      val work = true
      while (work && in.hasNext) {
        val n = in.next()
        n match {
          case x @ XmlEl(nm) if nm == name && (!noattr || x.attrs.size == 0) => {
            return Some(processor(untilEndTag(name)))
          }
          case _ => //do nothing
        }
      }
      None
    }
  }

  def trans[T](name: String)(body: XmlParseTransformer => T): T = {
    val work = true
    while (work && in.hasNext) {
      val n = in.next()
      n match {
        case XmlEl(nm) if nm == name => {
          return body(untilEndTag(name))
        }
        case _ => //do nothing
      }
    }
    throw new Exception("There wasn't tag " + name)
  }

  def textOf(name: String) = {
    def check(b: String, e: String) = {
      b == name && e == name
    }
    val tags = in.take(3).toList
    tags match {
      case XmlEl(ns) :: XmlText(t) :: XmlElEnd(ne) :: Nil if check(ns, ne) => t
      case XmlEl(ns) :: XmlERef(t) :: XmlElEnd(ne) :: Nil if check(ns, ne) => t
      case _ => throw new RuntimeException("there wasn't tag " + name + " but there was " + tags)
    }
  }

  def untilEndTag(name: String) = {
    new XmlParseTransformer(new CalculatingIterator[XmlData] {
      protected def calculate() = None

      override def chead = in.chead

      override def head = in.head

      override def next() = in.next()

      override def hasNext = chead match {
        case None => false
        case Some(XmlElEnd(nm)) if nm == name => false
        case _ => true
      }
    })
  }
}

object XmlParser {
  implicit def iterator2parsetransformer(in: CalculatingIterator[XmlData]) = new XmlParseTransformer(in)

  def parse(in: XMLEventReader) = new XmlIterator(in)
}
