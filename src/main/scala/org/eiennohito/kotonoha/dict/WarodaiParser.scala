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

import util.parsing.combinator.RegexParsers
import xml._

/**
 * @author eiennohito
 * @since 05.04.12
 */

case class Identifier(vol: Int, page: Int, num: Int)
case class Header(readings: List[String], writings: List[String], rusReadings: List[String], id: Identifier)
case class Card(header: Header, body: String)

object WarodaiParser extends RegexParsers {


  override def skipWhitespace = false

  def uint = regex("""[0-9]+""".r) ^^ (_.toInt)
  def sint = opt("+" | "-") ~> uint

  def endl = "\n\r" | "\r\n" | "\n"

  def ws = literal(" ").*

  def idwhat = (";" ~> uint) | ("(" ~> sint <~ (rep("," ~ sint) ~ opt(")")))

  def identifier = (("〔" ~> uint <~ (opt(" ") ~ ";" ~ opt(" "))) ~ uint ) ~ idwhat <~ "〕" ^^
    { case vol ~ page ~ entry =>  Identifier(vol, page, entry) }

  def rusReading = "(" ~> rep1sep( regex("[^\\,)]+".r), ", ") <~ ")"

  def reading = rep1sep(regex("[^,\\(【]+".r), ", ?".r)

  def writing = "【" ~> rep1sep("[^,】]+".r, ", ?".r) <~ "】"

  def header = "<i>".? ~> (((reading ~ opt(writing) <~ ws) ~ rusReading <~ ws) ~ identifier) <~ "</i>".? ^^ {
    case r ~ w ~ rr ~ i => Header(r, w.getOrElse(Nil), rr, i)
  }

  def body: Parser[String] = rep1sep("[^\\n]+".r, endl) ^^ {
    case body => body.map(_.trim).mkString("\n")
  }

  def fullcard = (header <~ endl) ~ body ^^ {
    case hdr ~ body => Card(hdr, body)
  }

  def emptycard : Parser[Card] = (header <~ guard(endl ~ endl)) map {
    case h: Header => Card(h, "")
  }

  def card = fullcard | emptycard

  def cards = rep1sep(card, repN(2, endl))
}

sealed trait Content {
  def toNodeSeq: NodeSeq
}

case object Endl extends Content {
  def toNodeSeq = <br/>
}

case class StringWrapper(str: String) extends Content {
  def toNodeSeq = Text(str)
}

case class Join(left: Content, right: Content) extends Content {
  def toNodeSeq = left.toNodeSeq ++ right.toNodeSeq
}

object JoinContent {
  def apply(left: Content, right: Content): Content = (left, right) match {
    case (StringWrapper(""), _) => right
    case (_, StringWrapper("")) => left
    case (_, _) => Join(left, right)
  }
}


case class Tag(name: String, content: Content) extends Content {
  def toNodeSeq = new Elem(null, name, Null, TopScope, content.toNodeSeq: _*)
}

object WarodaiBodyParser extends RegexParsers {
  override def skipWhitespace = false

  implicit def str2cont(s: String): Content = StringWrapper(s)

  def text = regex("[^<\\n\\r]+".r) ^^ (StringWrapper(_))

  def endl = "\n\r" | "\r\n" | "\n" ^^^ Endl

  def tagname = regex("[^>]".r)

  def tag : Parser[Content] = ("<" ~> tagname <~ ">") ~ text ~ ("</" ~> tagname <~ ">") into {
    case n1 ~ content ~ n2 => {
      if (n1 == n2) {
        success(Tag(n1, content))
      } else {
        failure("tag %s wasn't equal to %s".format(n1, n2))
      }
    }
  }

  def textline = (text ~ line) ^^ { case l ~ r => JoinContent(l, r)}
  def tagline = (tag ~ line) ^^ {case l ~ r => JoinContent(l, r)}
  def endline = guard(opt(endl)) ^^ {
    case Some(x) => Endl
    case None => StringWrapper("")
  }

  def line : Parser[Content] = (textline | tagline | endline)
  def body = rep1sep(line, endl) map {
    l =>
      def fnc(a: Content, b: Content) = JoinContent(a, b)
      l.reduce(fnc)
  }
}
