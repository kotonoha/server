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

package ws.kotonoha.server.web.snippet

import java.util.{ArrayList => JList}

import com.google.inject.Inject
import net.java.sen.SenFactory
import net.java.sen.dictionary.Token
import net.liftweb.http.{DispatchSnippet, S}
import ws.kotonoha.akane.analyzers.juman.JumanAnalyzer
import ws.kotonoha.akane.mecab.{InfoExtractor, MecabParser}
import ws.kotonoha.akane.parser.JumanPosSet
import ws.kotonoha.server.util.Strings

import scala.collection.JavaConversions._
import scala.xml.NodeSeq

/**
 * @author eiennohito
 * @since 24.03.12
 */

class MorphAna @Inject() (
  juman: JumanAnalyzer
) extends DispatchSnippet {
  val fact = SenFactory.getStringTagger(null, false)

  import ws.kotonoha.server.web.lift.Binders._

  val query = S.param("sentence").openOr("田中はいつもだるそうにしていた")

  override def dispatch = {
    case "query" => queryString
    case "parse" => parse
  }

  val queryString: NodeSeqFn = "* [value]" #> query

  def parseMecab(s: String) = {
    val p = new MecabParser()
    val items = p.parse(s)
    <tr><td colspan="6"><b>Mecab</b></td></tr> ++
    items.flatMap(i => {
      val ai = InfoExtractor.parseInfo(i)
      <tr>
        <td>{i.surf}</td>
        <td colspan="3">{Strings.substr(i.info, 60)}</td>
        <td>{ai.dicReading.getOrElse("-")}</td>
        <td>{ai.dicWriting.getOrElse("-")}</td>
      </tr>
    })
  }

  implicit class SString(val x: String) {
    def o: String = if (x == null || x.isEmpty) "*" else x
  }

  def parseJuman(s: String) = {
    val posset = JumanPosSet.default
    val data = juman.analyzeSync(s)

    bseq(data.get.lexemes) { l =>
      ";surface *" #> l.surface.o &
      ";reading *" #> l.reading.o &
      ";baseform *" #> l.baseform.o &
      ";pos *" #> posset.explain(l.posInfo).o &
      ";info *" #> l.options.map(o => s"${o.key}${o.value.map(x => ":"+x).getOrElse("")}").mkString(", ").o
    }
  }

  import scala.collection.JavaConverters._

  def parse(in: NodeSeq): NodeSeq = {
    val s = query

    if (s.length() > 2500) {
      return <b>Too long to be good, make it shorter than 2500 chars</b>
    }

    val jlist = new JList[Token]()
    val result = fact.analyze(s, jlist)
    val seq = bseq(result.asScala) { w =>
      ";surf *" #> w.getSurface &
      ";basic *" #> w.getMorpheme.getBasicForm &
      ";conj *" #> w.getMorpheme.getConjugationalForm &
      ";ctype *" #> w.getMorpheme.getConjugationalType &
      ";addit *" #> w.getMorpheme.getAdditionalInformation &
      ";read *" #> w.getMorpheme.getReadings.mkString(",") &
      ";writ *" #> w.getMorpheme.getPronunciations.mkString(",") &
      ";pos *" #> w.getMorpheme.getPartOfSpeech
    }

    val fn =
      ";gosen" #> seq &
      ";juman" #> parseJuman(s)

    //seq(in) /*++ parseMecab(s)*/ ++ parseJuman(s)
    fn(in)
  }

}
