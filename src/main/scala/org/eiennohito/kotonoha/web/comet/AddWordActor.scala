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

package org.eiennohito.kotonoha.web.comet

import net.liftweb.actor.LiftActor
import net.liftweb.http.CometActor
import org.eiennohito.kotonoha.actors.lift.AkkaInterop
import org.eiennohito.kotonoha.actors.ioc.ReleaseAkka
import akka.dispatch.Future
import org.eiennohito.kotonoha.records.dictionary.ExampleSentenceRecord
import akka.util.Timeout
import org.eiennohito.kotonoha.actors.{SearchQuery, RootActor}
import org.eiennohito.kotonoha.util.LangUtil
import org.eiennohito.kotonoha.actors.dict._
import net.liftweb.common.{Empty, Box}

/**
 * @author eiennohito
 * @since 29.04.12
 */

case class ExampleForSelection(ex: String, translation: Box[String])
case class DictData(data: List[DictCard])
case class DictCard(writing: String, reading: String, meaning: String)
case class WordData(dicts: List[DictData], examples: List[ExampleForSelection])

trait AddWordActorT extends CometActor with AkkaInterop  {
  lazy val root = akkaServ.root
  import org.eiennohito.kotonoha.actors.dict.DictType._
  import akka.util.duration._
  import akka.pattern.{ask, pipe}

  implicit val timeout = Timeout(2 seconds)

  def prepareWord(in: String): Future[WordData] = {
    val jf = (root ? DictQuery(jmdict, in, 5)).mapTo[SearchResult]
    val wf = (root ? DictQuery(warodai, in, 5)).mapTo[SearchResult]
    val exs = jf.flatMap { jmen => {
      val idsf = jmen.entries match {
        case Nil => { //don't have such word in dictionary
          root ? SearchQuery(in)
        }
        case x :: _ => {
          root ? SearchQuery(in + " " + x.readings.head)
        }
      }
      idsf.mapTo[List[Long]].flatMap { exIds => {
        val trsid = (root ? TranslationsWithLangs(exIds, LangUtil.langs)).mapTo[List[ExampleIds]]
        val exs = trsid.flatMap(root ? LoadExamples(_)).mapTo[List[ExampleEntry]]
        exs.map (_.map{ e => {
           ExampleForSelection(e.jap.content.is, e.other match {
             case x :: _ => x.content.valueBox
             case _ => Empty
           })
        }})
      }}
    }}

    def collapse(in: SearchResult) = {
      DictData(
        in.entries.map (e => {
          DictCard(
            e.writings.mkString(", "),
            e.readings.mkString(", "),
            e.meanings.mkString("\n")
          )
        })
      )
    }

    jf.zip(wf).zip(exs) map {
      case ((sr1, sr2), ex) => WordData(List(collapse(sr1), collapse(sr2)), ex)
    }
  }

  def render = {
    <a>help</a>
  }

  override def lowPriority = {
    case x: String =>
  }
}

class AddWordActor extends AddWordActorT with ReleaseAkka
