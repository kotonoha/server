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

import org.scalatest.FreeSpec
import org.scalatest.matchers.{MatchResult, BeMatcher, ShouldMatchers}
import util.parsing.input.CharSequenceReader
import ws.kotonoha.server.web.comet.Candidate

/**
 * @author eiennohito
 * @since 14.07.12
 */

class AddStringParserTest extends FreeSpec with ShouldMatchers {

  import AddStringParser._

  def sucv[T](v: T) = new BeMatcher[ParseResult[T]] {
    def apply(left: ParseResult[T]) = {
      left match {
        case Success(o, _) => MatchResult(
          o == v,
          "%s doesn't equal %s".format(o, v),
          "%s equals %s".format(o, v)
        )
        case _ => MatchResult(
          false,
          "Parse failure:\n" + left, ""
        )
      }
    }
  }

  def suc = new BeMatcher[ParseResult[_]] {
    def apply(left: ParseResult[_]): MatchResult = MatchResult(left.successful,
      "Parse failed: " + left, "Parse not failed" + left)
  }

  import language.implicitConversions

  implicit def str2charseqreader(in: String) = new CharSequenceReader(in)

  "addString parses" - {
    "comment" in {
      val cmt = "#whatever!"
      comment(cmt) should be(sucv(Some("whatever!"): Option[String]))
    }

    "word" in {
      word("馬鹿") should be(suc)
      word("馬#鹿") should not be (sucv("馬鹿"))
    }

    "simpleWord" in {
      simpleWord("馬鹿") should be(sucv(Candidate("馬鹿", None, None) :: Nil))
    }

    "fullWord" in {
      val data = "馬鹿｜ばか｜fool"
      fullWord(data) should be(sucv(Candidate("馬鹿", Some("ばか"), Some("fool")) :: Nil))
    }

    "word with reading" in {
      val data = "この野郎｜このやろう"
      wordWithReading(data) should be(sucv(Candidate("この野郎", Some("このやろう"), None) :: Nil))
    }

    "2 lines" in {
      val smt = "ばか\n駒"
      val res = entries(smt)
      res should be(sucv(
        Candidate("ばか", None, None) ::
          Candidate("駒", None, None) :: Nil
      ))
    }

    "everything" in {
      val data =
        """
          |馬鹿
          |この野郎｜このやろう
          |それでは｜вот так
          |そして｜そして｜как-то так
          |# это комметарий
          |% это тоже комметарий
          |
          |阿呆 % ахо-!
        """.stripMargin
      val res = entries(data)
      res should be(sucv(
        Candidate("馬鹿", None, None) ::
          Candidate("この野郎", Some("このやろう"), None) ::
          Candidate("それでは", None, Some("вот так")) ::
          Candidate("そして", Some("そして"), Some("как-то так")) ::
          Candidate("阿呆", None, None) :: Nil
      ))
    }
  }
}
