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

import net.liftweb.http.S
import net.liftweb.common.Full
import net.java.sen.SenFactory

import java.util.{ArrayList => JList}
import net.java.sen.dictionary.Token
import scala.collection.JavaConversions._
import xml.{Elem, NodeSeq}
import ws.kotonoha.akane.mecab.{InfoExtractor, MecabParser}
import ws.kotonoha.server.util.Strings
import ws.kotonoha.server.actors.ioc.{Akka, ReleaseAkka}
import ws.kotonoha.akane.ParsedQuery
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import ws.kotonoha.server.actors.interop.ParseSentence
import concurrent.duration.Duration

/**
 * @author eiennohito
 * @since 24.03.12
 */

object Parser extends Akka with ReleaseAkka {
  val fact = SenFactory.getStringTagger(null)

  import net.liftweb.util.Helpers._

  def data(in: NodeSeq): NodeSeq = {
    val s = S.param("sentence") openOr ""
    in map {
      case e: Elem => e % ("value" -> s)
    }
  }

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

  def parseJuman(s: String) = {
    val f = (akkaServ ? ParseSentence(s)).mapTo[ParsedQuery]
    val res = Await.result(f, Duration(5, TimeUnit.SECONDS))
    <tr><td colspan="6"><b>Juman</b></td></tr> ++
    res.inner.flatMap(f => {
      <tr>
        <td>{f.writing}</td>
        <td>{f.reading}</td>
        <td>{f.dictForm}</td>
        <td>{f.spPart}</td>
        <td colspan="2">{f.comment}</td>
      </tr>
    })
  }

  def parse(in: NodeSeq): NodeSeq = {
    S.param("sentence") match {
      case Full(s) => {
        if (s.length() > 2500) {
          return <b>Too long to be good, make it shorter than 2500 chars</b>
        }
        val jlist = new JList[Token]()
        val result = fact.analyze(s, jlist)
        val xml = result.flatMap( w =>
          bind("jw", in,
          "surf" -> w.getSurface,
          "basic" -> w.getMorpheme.getBasicForm,
          "conj" -> w.getMorpheme.getConjugationalForm,
          "ctype" -> w.getMorpheme.getConjugationalType,
          "addit" -> w.getMorpheme.getAdditionalInformation,
          "read" -> w.getMorpheme.getReadings.mkString(","),
          "writ" -> w.getMorpheme.getPronunciations.mkString(","),
          "pos" -> w.getMorpheme.getPartOfSpeech
          )
        )
        xml /*++ parseMecab(s)*/ ++ parseJuman(s)
      }
      case _ => <br />
    }
  }
}
