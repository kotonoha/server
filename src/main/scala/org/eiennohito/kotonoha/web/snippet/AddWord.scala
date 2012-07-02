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

import net.liftweb.http._
import js.JE.Call
import js.JsCmds.{SetHtml, Script, Function, RedirectTo}
import js.{JE, JsCmds, JsCmd, JsExp}
import net.liftweb.common.Full
import com.fmpwizard.cometactor.pertab.namedactor.InsertNamedComet
import org.eiennohito.kotonoha.util.unapply.XLong
import util.Random
import com.weiglewilczek.slf4s.Logging
import org.eiennohito.kotonoha.web.comet.{Cleanup, PrepareWords, WordList}
import org.eiennohito.kotonoha.records.{WordRecord, AddWordRecord, UserRecord}
import net.liftweb.json.JsonAST.{JObject, JValue}
import xml.{Text, NodeSeq}
import org.eiennohito.kotonoha.actors.ioc.{ReleaseAkka, Akka}
import org.eiennohito.kotonoha.actors.RootActor
import org.eiennohito.kotonoha.actors.model.MarkAllWordCards
import org.eiennohito.kotonoha.records.dictionary.{JMString, JMDictRecord}
import org.eiennohito.kotonoha.util.LangUtil

/**
 * @author eiennohito
 * @since 17.03.12
 */

case class Candidate(writing: String, reading: Option[String], meaning: Option[String]) {
  def toQuery: JValue = {
    import org.eiennohito.kotonoha.util.KBsonDSL._
    //def regex(s: String) = "$regex" -> ("^" + s + "$")

    ("writing.value" -> writing) ~ (reading map { r => ("reading.value" -> r) } )
  }
}

case class InvalidStringException(str: String) extends Exception("String " + str + " is not valid")

object Candidate {
  def wrap(s1: String) = {
    val s = s1.trim
    if (s == null || s.equals("")) {
      None
    } else {
      Some(s)
    }
  }
  def apply(in: String) = {
    in.split("[|｜]", 3) match {
      case Array(w) => new Candidate(w, None, None)
      case Array(w, r, m) => new Candidate(w, wrap(r), wrap(m))
      case Array(w, smt) => {
        import org.eiennohito.kotonoha.util.UnicodeUtil._
        if (stream(smt).forall(isKana(_))) {
          new Candidate(w, wrap(smt), None)
        } else {
          new Candidate(w, None, wrap(smt))
        }
      }
      case _ => throw new InvalidStringException(in)
    }
  }
}

object AddWord extends Logging with Akka with ReleaseAkka {
  import net.liftweb.util.Helpers._

  object data extends RequestVar[String]("")
  object good extends RequestVar[List[Candidate]](Nil)

  def all(s: String): List[Candidate] = s.split("\n").map(_.trim).filter(_.length != 0).map(Candidate(_)).toList

  def addField(in: NodeSeq): NodeSeq = {
    def process(d: List[Candidate]) = {
      logger.debug("trying to add words from " + d)
      val opid = Random.nextLong()
      val recs = d.map(d => {
        val rec = AddWordRecord.createRecord
        rec.writing(d.writing).meaning(d.meaning).reading(d.reading).processed(false).group(opid).user(UserRecord.currentId)
        rec.save
      })

      RedirectTo("/words/approve_added?list="+opid.toHexString)
      //SetHtml("asdf", <a href="http://google.com">Google</a>)
    }

    bind("word", SHtml.ajaxForm(in),
      "data" -> SHtml.textarea(data.is, data(_), "id" -> "word-area"),
      "all" -> SHtml.ajaxSubmit("Add all", () => process(all(data.is))),
      "nonproblem" -> SHtml.ajaxSubmit("Add only new", () => process(good.is)), "class" -> "default")
  }

  def anotherSnippet(in: NodeSeq): NodeSeq = {
    S.runTemplate("templates-hidden" :: "test" :: Nil) match {
      case Full(x) => x
      case _ => <em>Error in rendering template test</em>
    }
  }

  def penaltizeWord(w: WordRecord): JsCmd = {
    akkaServ ! MarkAllWordCards(w.id.is, 1)
    JsCmds.Noop
  }

  case class RenderData(candidate: Candidate, present: List[WordRecord], dicEn: Option[JMDictRecord]) {
    def renderDic(): NodeSeq = {
      dicEn match {
        case None => Nil
        case Some(e) =>
            <div class="nihongo">{ e.reading.is.map(_.value.is).headOr("") }</div>
            <div> {e.meaning.is.flatMap(_.vals.is.filter{x => LangUtil.okayLang(x.loc)}.map(_.str)).headOr("") }</div>
      }

    }

    def badness = present match {
      case Nil => "good"
      case _ => "bad"
    }

    def renderPresent: NodeSeq = present flatMap { w =>
      <div class="writing nihongo">{w.writing.is}</div>
      <div class="reading nihongo">{w.reading.is}</div>
      <div class="meaning">{w.meaning.is}</div> ++
      SHtml.a(() => penaltizeWord(w), Text("Penaltize word"))
    }
  }

  def render(d: RenderData): NodeSeq = {
    val word = <div class={d.badness + " nihongo"}>{d.candidate.writing}</div>
            <div class="already-present">{d.renderPresent}</div> ;
    <tr class="word-container">
      <td class="word-cell">{word}</td>
      <td class="dic-cell">{d.renderDic()}</td>
    </tr>
  }

  def findDescription(c: Candidate, m: Map[String, List[JMDictRecord]]): Option[JMDictRecord] = {
    m.get(c.writing) flatMap (l => {
      c.reading match {
        case None => l.headOption
        case Some(s) => l.filterNot(_.reading.is.find(_.value.is.equals(s)).isEmpty).headOption
      }
    })
  }

  def handleWordData(in: String): JsCmd = {
    import org.eiennohito.kotonoha.util.KBsonDSL._
    data(in)
    val lines = all(in)
    val html = if (lines.isEmpty) {
      Text("")
    } else {
      val patterns = lines map { _.toQuery }
      val q: JObject = ("user" -> UserRecord.currentId.get) ~ ("$or" -> (lines map { c => "writing" -> bre(c.writing)}))
      val dicQ: JObject = "$or" -> patterns
      val ws = WordRecord.findAll(q).groupBy{w => w.writing.is} withDefaultValue(Nil)
      val dics = JMDictRecord.findAll(dicQ).toArray
      val des = dics.map { de => de.writing.is.map{jms => jms.value.is -> List(de) } toMap }.
        foldLeft(Map[String, List[JMDictRecord]]()) {
        case (m1, m2) => {
          var m = m1
          for ((k, v) <- m2) {
            m.get(k) match {
              case Some(l) => m  += k -> (l ++ v).distinct
              case None => m += k -> v
            }
          }
          m
        }
      }
      val data = lines map {
        l => RenderData(l, ws(l.writing), findDescription(l, des))
      }
      good(data.filter(_.present.isEmpty).map(_.candidate))
      data flatMap (render(_))
    }
    SetHtml("similar-words", html) & Call("finish").cmd
  }

  def asyncFunc(in: NodeSeq): NodeSeq = {
    val af = SHtml.ajaxCall(JE.JsVar("text"), AjaxContext.js(Full("finish"), Full("finish")), handleWordData(_))
    val body: JsCmd = af._2.cmd
    val jsExp: JsCmd = Function("displaySimilar", "text" :: Nil, body)
    Script(jsExp)
  }
}

object AddWordActorSnippet extends InsertNamedComet {

  /**
   * These are the two val(s) you would have to
   * override after extending this trait.
   * No need to touch the render method (I hope)
   */
  def cometClass = "AddWordActor"

  override def name = {
    S.param("list") match {
      case Full(XLong(lid)) => "list"+lid
      case _ => "user" + UserRecord.currentUserId.openOr(super.name)
    }
  }

  override def messages = {
    Cleanup :: (S.param("list") match {
      case Full(XLong(id)) => WordList(id) :: Nil
      case _ => PrepareWords :: Nil
    })
  }
}
