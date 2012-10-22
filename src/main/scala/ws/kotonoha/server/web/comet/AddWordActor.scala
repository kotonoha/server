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

package ws.kotonoha.server.web.comet

import ws.kotonoha.server.actors.lift.AkkaInterop
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import akka.util.Timeout
import com.fmpwizard.cometactor.pertab.namedactor.NamedCometActor
import ws.kotonoha.server.records._
import ws.kotonoha.server.util.DateTimeUtils
import com.weiglewilczek.slf4s.Logging
import net.liftweb.http.js.jquery.JqJsCmds.{Hide, Show}
import net.liftweb.common.{Empty, Box}
import ws.kotonoha.server.actors.model._
import net.liftweb.http.{S, SHtml, CometActor}
import net.liftweb.http.js.JsCmds.RedirectTo
import xml.NodeSeq
import net.liftweb.json.JsonAST._
import net.liftweb.http.S.AFuncHolder
import net.liftweb.http.js.JE.{JsObj, FormToJSON}
import net.liftweb.http.js.{JE, JsCmds, JsCmd}
import akka.actor.Props
import scala.Left
import scala.Some
import xml.Text
import net.liftweb.json.JsonAST.JString
import net.liftweb.http.js.JsCmds.SetHtml
import ws.kotonoha.server.actors.UpdateRecord
import net.liftweb.common.Full
import scala.Right
import ws.kotonoha.server.actors.model.DictData
import net.liftweb.json.JsonAST.JArray
import ws.kotonoha.server.actors.model.WordData
import net.liftweb.json.JsonAST.JField
import net.liftweb.http.RenderOut
import ws.kotonoha.server.actors.model.RegisterWord

/**
 * @author eiennohito
 * @since 29.04.12
 */



case class WordList(id: Long)
case object PrepareWords
case object DisplayNext
case class WordCount(current: Int, total: Int)
case class DoRenderAndDisplay(wd: WordData)
case object Cleanup

class TimeoutException extends RuntimeException

trait AddWordActorT extends NamedCometActor with AkkaInterop with Logging {
  lazy val root = akkaServ.root
  val self = this
  import akka.util.duration._
  import akka.pattern.ask
  import com.foursquare.rogue.Rogue._
  import DateTimeUtils._

  private implicit val timeout = Timeout(10 seconds)
  private var list: Box[WordList] = Empty
  private var selector: WordDataCalculator = _

  lazy val wordCreator = createActor(Props[WordCreateActor])

  def prepareWord(in: AddWordRecord) = {
    val f = (wordCreator ? CreateWordData(in)).mapTo[WordData]
    f
  }

  def button(code: String, value: String) = {
    val pair = "code" -> JE.Str(code)
    <button onclick={jsonSend.apply(FormToJSON("main-form"), JsObj(pair)).toJsCmd}>{value}</button>
  }

  def content = {
    val btnNs: NodeSeq = <span>{button("save", "Save & Next")}</span><span>{button("mark", "Mark as temp")}</span><span>{button("skip", "Skip this word")}</span> ;

    val cssF = "#word-card *" #> "" &
                "#word-examples *" #> "" &
                "#dictionary-entries *" #> "" &
                "#button-pane *" #> btnNs

    cssF(defaultHtml)
  }

  def withS[T](f: => T): (T, Map[String, AFuncHolder]) = {
    val oldMap = S.functionMap
    S.clearFunctionMap
    val x = try {
      val x = f
      val map = S.functionMap
      (x, map)
    } finally {
      S.clearFunctionMap
      oldMap foreach {case (n, f) => S.addFunctionMap(n, f)}
    }
    x
  }


  def render = {
    logger.debug("AddWordActor.render")
    RenderOut(Full(content), Empty, Full(Hide("button-pane", 5 millis) & jsonToIncludeInCode), Empty, true)
  }

  var displaying: Box[WordData] = Empty
  var curMap: Map[String, AFuncHolder] = Map()

  def saveAndNext(stat: WordStatus.Value = WordStatus.Approved): Unit = {
    logger.debug("Calling save!")
    displaying match {
      case Full(w) => {
        w.word.writing.valueBox match {
          case Empty => w.word.writing(w.word.reading.is)
          case Full(Nil) => w.word.writing(w.word.reading.is)
          case _ => //
        }
        val f = root ? RegisterWord(w.word, stat)
        f map { _ => w.onSave.tryComplete(Right(w)) }
      }
      case _ => status("Have no more words");
    }
  }

  def skip(): Unit = {
    logger.debug("Skipping current word")
    displaying map { w => w.onSave.tryComplete(Right(w)) }
  }

  def withId(in: Box[NodeSeq], id: String) = {
    val tf = "input [id]" #> id & "textarea [id]" #> id
    in map { tf(_) }
  }

  def renderWord(w: WordRecord) = {
    var in: NodeSeq = Nil
    val css = "#word-card *" #> {x => in = x; x}
    css(defaultHtml)
    bind("word", in,
       "writing" -> withId(w.writing.toForm, "word-writing"),
       "reading" -> withId(w.reading.toForm, "word-reading"),
       "meaning" -> withId(w.meaning.toForm, "word-meaning"))
  }

  def addExample(w: WordRecord): JsCmd = {
    val exs = w.examples.is
    w.examples(exs ++ List(ExampleRecord.createRecord))
    val (html, map) = withS { renderExamples(w) }
    curMap = curMap ++ map
    partialUpdate(SetHtml("word-examples", html))
    JsCmds.Noop
  }

  def checked(in: Seq[ExampleRecord], w: WordRecord) = {
    val c = w.writing.stris(0)
    in.filter(_.example.is.contains(c))
  }

  def renderExamples(w: WordRecord): NodeSeq = {
    val ex = w.examples.is
    val objs = SHtml.checkbox[ExampleRecord](ex, checked(ex, w), l => { w.examples(l.toList); logger.debug("Examples should be" + l)})

    val ns = objs.flatMap( o => <tr>
      <td class="checkbox-cell">{o.xhtml}</td>
      <td class="nihongo fifty">{o.key.example.toForm.get}</td>
      <td class="fifty">{o.key.translation.toForm.get}</td>
    </tr>) ++
    <tr>
      <td></td><td></td>
      <td class="fifty">
        <button onclick={jsonSend(JsObj("func" -> JE.Str("add_example"))).toJsCmd + "return false;"}>Add new example</button>
      </td>
    </tr>

    return <table>{ns}</table> //keyword return is needed to separate 2 xml literal seqs
  }

  def renderDicts(dicts: List[DictData]) = {
    dicts.flatMap(d => <div class="dict-name">{d.name}</div> ++
      d.data.flatMap(e =>
      <div onclick="javascipt:copyToWord(this);return false;" class="dict-entry">
        <span class="nihongo">{e.writing}</span>
        <span class="nihongo">({e.reading})</span>
        <span class="block">{e.meaning}</span>
      </div>
    ))
  }

  def executor = akkaServ.context

  def status(s: String) {
    partialUpdate(SetHtml("status-string", Text(s)))
  }

  private class WordDataCalculator extends Logging {
    private def q = list match {
      case Full(l) => {
        AddWordRecord where (_.user eqs UserRecord.currentId.get) and (_.group eqs l.id) and (_.processed eqs false)
      }
      case _ => {
        AddWordRecord where (_.user eqs UserRecord.currentId.get) and (_.processed eqs false)
      }
    }

    private var items = q.fetch()
    private var cur_ = 0
    def cur = cur_

    val total = items.size

    def next = {
      items match {
        case Nil => None
        case _ => {
          val f = calculate(items.head)
          cur_ += 1
          items = items.tail
          self ! WordCount(cur, total)
          Some(f)
        }
      }
    }

    protected def calculate(item: AddWordRecord) =
    prepareWord(item) map (w => {
      w.word.tags(item.tags.is).writing(item.writing.valueBox).user(item.user.valueBox)
      item.reading.valueBox map {r => w.word.reading(r) }
      item.meaning.valueBox map {m => w.word.meaning(m) }
      w.onSave map {
        i => root ! UpdateRecord(item.processed(true))
      }
      w
    })
  }

  def prepare() = {
    logger.debug("prepare is called")
    selector = new WordDataCalculator()
    self ! DisplayNext
  }


  def renderJS(data: WordData): JsCmd = {
    val (cmd, map) = withS {
      val wordHtml = renderWord(data.word)
      val exHtml = renderExamples(data.word)
      val dictHtml = renderDicts(data.dicts)
      SetHtml("word-card", wordHtml) &
      SetHtml("word-examples", exHtml) &
      SetHtml("dictionary-entries", dictHtml) &
      Show("button-pane", 50 millis)
    }
    curMap = map
    cmd
  }

  def displayNext(): Unit = {
    status("")
    logger.debug("Trying to display next word!")
    val n = selector.next
    n match {
      case Some(f) => {
        f.onComplete {
          case Right(wd) => self ! DoRenderAndDisplay(wd)
          case Left(e) => logger.error("Error in displaying word", e)
        }
      }
      case None => {
        if (!list.isEmpty) {
          partialUpdate(RedirectTo("/words/add"))
        } else {
          status("Have no more words to add.")
        }
      }
    }
  }

  def doProcess(code: String, data: WordData): Unit = {
    code match {
      case "save" => saveAndNext()
      case "mark" => saveAndNext(WordStatus.ReviewWord)
      case "skip" => skip()
    }
    curMap = Map()
    displaying = Empty
    val action =  SetHtml("word-card", Text("")) &
          SetHtml("word-examples", Text("")) &
          SetHtml("dictionary-entries", Text("")) &
          Hide("button-pane", 50 millis)
    partialUpdate(action)

    self ! DisplayNext
  }


  case class CallFunc(name: String)
  def jFunc: String => Unit = {
    case "add_example" => displaying map {d => addExample(d.word)}
  }

  def process(js: JValue) = {
    (js \ "command").children.foreach {
      case JField("func", JString(fnc)) => self ! CallFunc(fnc)
      case JField(name, JString(s)) => curMap.get(name) map {_(List(s))}
      case JField(name, JArray(l)) => {
        val ls = l.collect {case JString(s) => s}
        val n = name
        curMap.get(n) map {_(ls)}
      }
      case x@_ => logger.debug("Ignored json:" + x)
    }
    (js \ "params").children.foreach {
      case JField("code", JString(s)) => displaying map { doProcess(s, _) }
      case x@_ => logger.debug("Ignored json:" + x)
    }
  }

  case class ProcessJson(obj: JValue)

  override def receiveJson = {
    case o => self ! ProcessJson(o); JsCmds.Noop
  }

  override def lowPriority = {
    case x: String => println(x)
    case PrepareWords => list = Empty; prepare()
    case l: WordList => list = Full(l); prepare()
    case WordCount(cur, tot) => partialUpdate(SetHtml("counter", <span>Now displaying {cur} of {tot}</span>))
    case DisplayNext => displayNext()
    case b : Boolean => //ogger.debug(S.inStatefulScope_?)
    case DoRenderAndDisplay(wd) => {
      val js = renderJS(wd)
      displaying = Full(wd);
      partialUpdate(js)
    }
    case ProcessJson(js) => process(js)
    case CallFunc(fnc) => jFunc(fnc)
  }

  override def highPriority = {
    case Cleanup => logger.info("Cleaning actor up"); displaying = Empty
  }
}

class AddWordActor extends CometActor with AddWordActorT with ReleaseAkka
