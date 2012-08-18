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

package ws.kotonoha.server.util.parsing

import util.parsing.combinator.RegexParsers
import ws.kotonoha.server.web.snippet.Candidate
import ws.kotonoha.server.util.Strings

/**
 * @author eiennohito
 * @since 14.07.12
 */

object AddStringParser extends RegexParsers {
  override def skipWhitespace = false

  def space = regex("[ 　]*".r)

  def eoi = new Parser[Unit] {
    def apply(in: AddStringParser.Input) = {
      if (in.atEnd) Success((), in) else Failure("End of input expected", in)
    }
  }

  def endl = regex("(\n\r)|(\r\n)|(\n)".r)

  def sep = ((regex("[|｜]".r)))

  def wssep = space ~ sep ~ space

  def comment =  opt( "[%#]".r ~> "[^\n\r]*".r  ) <~ (endl | eoi)

  def readingString = (regex("[\u3040-\u3097,、]+".r)) ^^ { Strings.trim(_) } //kana and ,s

  def word = (regex("[^|｜%#\n\r]+".r)) ^^ { Strings.trim(_) }

  def skippedLine = "[^\n\r]*".r <~ guard(comment) ^^^ Nil

  def commentLine = (space) <~ guard(comment) ^^^ Nil

  def simpleWord = word <~ guard(comment) ^^ { Candidate(_, None, None) :: Nil }

  def wordWithReading = ((word <~ wssep) ~ readingString) <~ guard(comment) ^^
    {case w ~ r => Candidate(w, Some(r), None) :: Nil }

  def wordWithMeaning = ((word <~ wssep) ~ word) <~ guard(comment) ^^
    {case w ~ m => Candidate(w, None, Some(m)) :: Nil }

  def fullWord = ((word <~ wssep) ~ (word <~ wssep) ~ word) <~ guard(comment) ^^
    {case w ~ r ~ m => Candidate(w, Some(r), Some(m)) :: Nil}

  def line = simpleWord | wordWithReading | wordWithMeaning | fullWord | commentLine | skippedLine

  def commentedLine = (space ~> line) <~ (space ~ comment)

  def entries = phrase(rep(not(eoi) ~> commentedLine)) ^^ { _.flatten }

}
