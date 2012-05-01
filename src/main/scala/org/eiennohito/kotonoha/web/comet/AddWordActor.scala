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
import org.eiennohito.kotonoha.actors.lift.AkkaInterop
import org.eiennohito.kotonoha.actors.ioc.ReleaseAkka
import akka.dispatch.Future
import org.eiennohito.kotonoha.records.dictionary.ExampleSentenceRecord
import akka.util.Timeout
import org.eiennohito.kotonoha.actors.dict._
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.{JsCmds, JsCmd}
import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import net.liftweb.common.{Full, Empty, Box}
import xml.{Text, NodeSeq}
import net.liftweb.util.Helpers
import org.eiennohito.kotonoha.xml.CalculatingIterator
import org.eiennohito.kotonoha.model.events.AddWordEvent
import org.eiennohito.kotonoha.records._
import org.eiennohito.kotonoha.util.{DateTimeUtils, Formatting, LangUtil}
import net.liftweb.http.js.JsCmds.{RedirectTo, SetHtml}
import net.liftweb.http.js.jquery.JqJsCmds.Show
import org.eiennohito.kotonoha.actors.{SaveRecord, UpdateRecord, SearchQuery, RootActor}
import com.weiglewilczek.slf4s.Logging
import org.omg.CORBA._PolicyStub

/**
 * @author eiennohito
 * @since 29.04.12
 */

case class ExampleForSelection(ex: String, translation: Box[String], id: Long)
case class DictData(name: String, data: List[DictCard])
case class DictCard(writing: String, reading: String, meaning: String)
case class WordData(dicts: List[DictData], examples: List[ExampleForSelection], word: WordRecord)

case class WordList(id: Long)
case object PrepareWords
case object DisplayNext
case class WordCount(current: Int, total: Int)

trait AddWordActorT extends NamedCometActor with AkkaInterop with Logging {
  lazy val root = akkaServ.root
  val self = this
  import org.eiennohito.kotonoha.actors.dict.DictType._
  import akka.util.duration._
  import akka.pattern.{ask, pipe}
  import com.foursquare.rogue.Rogue._
  import DateTimeUtils._

  private implicit val timeout = Timeout(2 seconds)
  private var list: Box[WordList] = Empty
  private var selector: CalculatingIterator[Future[WordData]] = _

  def createWord(dictData: List[DictData], examples: List[ExampleForSelection]): WordRecord = {
    val rec = WordRecord.createRecord
    rec.user(UserRecord.currentId).status(WordStatus.New).createdOn(DateTimeUtils.now)

    val exs = examples.map { e =>
      ExampleRecord.createRecord.id(e.id).example(e.ex).translation(e.translation.openOr(""))
    }
    rec.examples(exs)

    val data = dictData.flatMap(d => d.data).headOption
    data match {
      case Some(d) => rec.writing(d.writing).reading(d.reading).meaning(d.meaning)
      case _ => rec.writing("").reading("").meaning("")
    }
    rec
  }

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
           }, e.jap.id.is)
        }})
      }}
    }}

    def collapse(in: SearchResult, name: String) = {
      DictData( name,
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
      case ((sr1, sr2), ex) => {
        val dicts = List(collapse(sr1, "JMDict"), collapse(sr2, "Warodai"))
        WordData(dicts, ex, createWord(dicts, ex))
      }
    }
  }

  def render = {
    "#word-card *" #> "" &
    "#word-examples *" #> "" &
    "#dictionary-entries *" #> "" &
    "#button-pane *" #> <span>{SHtml.ajaxButton("Save & Next", () => saveAndNext())}</span>
  }

  def saveAndNext(): JsCmd = {
    selector.chead match {
      case Some(f) => f.map (w => {
        w.word.status(WordStatus.Approved)
        root ! UpdateRecord(w.word)
      })
      case _ => status("Have no more words");
    }
    self ! DisplayNext
    JsCmds.Noop
  }

  def withId(in: Box[NodeSeq], id: String) = {
    val tf = "input [id]" #> id
    in map { tf(_) }
  }

  def renderWord(w: WordRecord) = {
    var in: NodeSeq = Nil
    "#word-display" #> {x => in = x; x}
    bind("word", SHtml.ajaxForm(in),
       "writing" -> withId(w.writing.toForm, "word-writing"),
       "reading" -> withId(w.reading.toForm, "word-reading"),
       "meaning" -> withId(w.meaning.toForm, "word-meaning"))
  }

  def addExample(w: WordRecord): JsCmd = {
    val exs = w.examples.is
    w.examples(exs ++ List(ExampleRecord.createRecord))
    partialUpdate(SetHtml("extable", renderExamples(w)))
    JsCmds.Noop
  }

  def renderExamples(w: WordRecord): NodeSeq = {
    val ex = w.examples.is
    val ns = ex.flatMap( ex => <tr><td class="nihongo full" width="50%">{ex.example.toForm}</td>
      <td class="full" width="50%">{ex.translation.toForm}</td>
    </tr>) ++
    <tr>
      <td></td>
      <td>
        {SHtml.ajaxSubmit("Add new example", () => addExample(w))}
      </td>
    </tr>

    return <table>{ns}</table> //keyword return is needed to separate 2 xml literal seqs
  }

  def renderDicts(dicts: List[DictData]) = {
    dicts.flatMap(d => <div>{d.name}</div> ++
      d.data.flatMap(e =>
      <div onclick="javascipt:copyToWord(this);return false;">
        <span class="nihongo">{e.writing}</span>
        <span class="nihongo">({e.reading})</span>
        <span>{e.meaning}</span>
      </div>
    ))
  }

  def status(s: String) {
    partialUpdate(SetHtml("status-string", Text(s)))
  }

  def prepare() = {
    val q = list match {
      case Full(l) => {
        AddWordRecord where (_.user eqs UserRecord.currentId.get) and (_.group eqs l.id) and (_.processed eqs false)
      }
      case _ => {
        AddWordRecord where (_.user eqs UserRecord.currentId.get) and (_.processed eqs false)
      }
    }
    selector = new CalculatingIterator[Future[WordData]] {
      lazy val items = q.fetch().toArray
      var cur = 0

      protected def calculate() = {
        logger.debug("have following items to display:" + items)
        logger.debug("now position " + cur)
        if (cur == items.length) {
          None
        } else {
          self ! WordCount(cur + 1, items.length)
          val item = items(cur)
          root ! UpdateRecord(item.processed(true))
          cur += 1
          Some(prepareWord(item.content.is) map (w => {
            w.word.tags(item.tags.is);
            root ! SaveRecord(w.word)
            w
          }))
        }
      }
    }
    self ! DisplayNext
  }


  def displayWord(data: WordData): Unit = {
    val wordHtml = renderWord(data.word)
    val exHtml = renderExamples(data.word)
    val dictHtml = renderDicts(data.dicts)
    val cmd = SetHtml("word-display", wordHtml) &
      SetHtml("word-examples", exHtml) &
      SetHtml("dictionary-entries", dictHtml) &
      Show("button-pane", 50 millis)
    logger.debug("Calculated html for user:" + wordHtml)
    partialUpdate(cmd)
  }

  def displayNext(): Unit = {
    status("")
    logger.debug("Trying to display next word!")
    if (selector.hasNext) {
      val next = selector.next()
      logger.debug("We have next, displaying it!")
      next.map(displayWord(_))
    } else {
      if (!list.isEmpty) {
        partialUpdate(RedirectTo("/words/add"))
      } else {
        status("Have no more words to add.")
      }
    }
  }

  override def lowPriority = {
    case x: String => println(x)
    case PrepareWords => list = Empty; prepare()
    case l: WordList => list = Full(l); prepare()
    case WordCount(cur, tot) => partialUpdate(SetHtml("counter", <span>Now displaying {cur} of {tot}</span>))
    case DisplayNext => displayNext()
  }
}

class AddWordActor extends AddWordActorT with ReleaseAkka
