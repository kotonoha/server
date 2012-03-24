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

package org.eiennohito.kotonoha.web.snippet

import net.liftweb.http.S
import net.liftweb.common.{Full, Empty}
import net.java.sen.SenFactory

import java.util.{ArrayList => JList}
import net.java.sen.dictionary.Token
import scala.collection.JavaConversions._
import xml.{Elem, NodeSeq}

/**
 * @author eiennohito
 * @since 24.03.12
 */

object Parser {
  val fact = SenFactory.getStringTagger

  import net.liftweb.util.Helpers._

  def data(in: NodeSeq): NodeSeq = {
    val s = S.param("sentence") openOr ""
    in map {
      case e: Elem => e % ("value" -> s)
    }
  }

  def parse(in: NodeSeq): NodeSeq = {
    S.param("sentence") match {
      case Full(s) => {
        if (s.length() > 500) {
          return <b>Too long to be good, make it shorter than 500 chars</b>
        }
        val jlist = new JList[Token]()
        val result = fact.analyze(s, jlist)
        val xml = result.map( w =>
          bind("jw", in,
          "surf" -> w.getSurface,
          "basic" -> w.getMorpheme.getBasicForm,
          "conj" -> w.getMorpheme.getConjugationalForm,
          "read" -> w.getMorpheme.getReadings.mkString(","),
          "writ" -> w.getMorpheme.getPronunciations.mkString(","),
          "pos" -> w.getMorpheme.getPartOfSpeech
          )
        )
        xml.reduce(_++_)
      }
      case _ => <br />
    }
  }
}
