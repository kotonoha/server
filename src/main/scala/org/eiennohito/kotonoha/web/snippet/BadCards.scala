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

import xml.NodeSeq
import org.eiennohito.kotonoha.actors.ioc.{Akka, ReleaseAkka}
import org.eiennohito.kotonoha.records.{UserSettings, WordRecord, UserRecord}
import org.eiennohito.kotonoha.actors.learning.{WordsAndCards, LoadReviewList}
import akka.dispatch.Await
import net.liftweb.util.CssSel
import net.liftweb.http.{SHtml, RequestVar, S, DispatchSnippet}
import net.liftweb.common.Full
import org.eiennohito.kotonoha.util.unapply.XInt
import org.eiennohito.kotonoha.actors.{UpdateRecord, SaveRecord}
import org.eiennohito.kotonoha.util.StrokeType
import collection.mutable.ListBuffer
import annotation.tailrec

/**
 * @author eiennohito
 * @since 27.06.12
 */

object BadCards extends DispatchSnippet with Akka with ReleaseAkka  {
  import net.liftweb.util.Helpers._
  import akka.util.duration._


  def dispatch = {
    case "surround" => surround
    case "words" => words
    case "selector" => selector
    case "stripped" => str
    case "words2" => words2
  }

  object stripped extends RequestVar[Boolean](S.param("stripped") match {
    case Full("true") => true
    case _ => false
  })

  object maxRecs extends RequestVar[Int] ({
    val cur = UserSettings.current
    cur.badCount.valueBox openOr(20)
  })

  def str(in: NodeSeq): NodeSeq = {
    val s = S.attr("s") match {
      case Full("false") => false
      case _ => true
    }
    if (s == stripped.is) in else Nil
  }

  def selector(in: NodeSeq): NodeSeq = {
    val lst = (maxRecs.is :: List(10, 20, 30, 40, 50)).distinct.sorted
    val ents = lst map {_.toString} map {c => c -> c}
    def onSubmit(cnt: String): Unit = {
      cnt match {
        case XInt(c1) => {
          val c = c1 min 75
          val cur = UserSettings.current.badCount(c)
          akkaServ ! UpdateRecord(cur)
          maxRecs.set(c)
        }
        case _ => ///
      }
    }
    val ns = SHtml.untrustedSelect(ents, Full(maxRecs.is.toString), onSubmit)
    ("select" #> ns) (in)
  }

  def surround(in: NodeSeq): NodeSeq = {
    if (stripped.is) { in }
    else { <lift:surround with="default" at="content">{in}</lift:surround> }
  }

  def sod(in: String) = {
    Kakijyun.japSod(in, sodType)
  }

  def kanjSod(in: String) = {
    Kakijyun.kanjiSod(in, sodType)
  }

  def sodType = {
    if (stripped.is) StrokeType.Png500
    else StrokeType.Png150
  }

  def words(in: NodeSeq): NodeSeq = {
    val uid = UserRecord.currentId.openTheBox
    val obj = (akkaServ ? LoadReviewList(uid, maxRecs.is)).mapTo[WordsAndCards]
    val res = Await.result(obj, 5.0 seconds)

    def tf(w: WordRecord) = {
      val css: CssSel = (".data *" #>
        <div>{w.writing.is}</div>
        <div>{w.reading.is}</div>) &
      ".mean *" #> <span>{w.meaning.is}</span> &
      ".sod *" #> <span>{sod(w.writing.is)}</span>
      css(in)
    }

    res.words.flatMap(w => tf(w))
  }

  def words2(in: NodeSeq): NodeSeq = {
    val uid = UserRecord.currentId.openTheBox
    val obj = (akkaServ ? LoadReviewList(uid, maxRecs.is)).mapTo[WordsAndCards]
    val res = Await.result(obj, 5.0 seconds)

    val wds = asRows(res.words)
    val ns = rowsToTable(wds)

    ("table *" #> ns) (in)
  }

  trait WordRenderer
  trait CellRenderer extends WordRenderer {
    def render: NodeSeq
  }

  trait RowRenderer extends WordRenderer {
    def render: List[NodeSeq]
  }

  class TwoCellRenderer(left: CellRenderer, right: CellRenderer) extends RowRenderer {
    def render = List(left.render, right.render)
  }

  object NullRenderer extends CellRenderer {
    def render = Nil
  }

  class SimpleWordCell(w: WordRecord) extends CellRenderer {
    def render =
      <div>
        <div class="rd">{w.reading.is}</div>
        <div class="sod">{sod(w.writing.is)}</div>
        <div class="mn">{w.meaning.is}</div>
      </div>
  }

  class SimpleWordRow(w: WordRecord) extends RowRenderer {

    def left =
      <div>
        <div>
          <span class="wr">{w.writing.is}</span>
          <span class="rd">{w.reading.is}</span>
        </div>
        <div class="sod">{kanjSod(w.writing.is)}</div>
      </div>

    def right = <div class="mn">{w.meaning.is}</div>

    def render = List(left, right)
  }

  object SimpleSmall {
    def unapply(w: WordRecord): Option[SimpleWordCell] = {
      val b1 = w.reading.is.length < 150
      val b2 = w.writing.is.length < 6
      if (b1 && b2) {
        Some(new SimpleWordCell(w))
      } else None
    }
  }

  def asRows(in: List[WordRecord]) = {
    val rows = new ListBuffer[RowRenderer]
    val cells = new ListBuffer[CellRenderer]
    in.foreach {
      case SimpleSmall(c) => cells += c
      case w => rows += new SimpleWordRow(w)
    }

    def rec(lst: List[CellRenderer]): List[RowRenderer] = {
      lst match {
        case a :: b :: xs => new TwoCellRenderer(a, b) :: rec(xs)
        case a :: Nil => new TwoCellRenderer(a, NullRenderer) :: Nil
        case Nil => Nil
      }
    }
    rows.toList ++ rec(cells.toList)
  }

  def rowsToTable(in: List[RowRenderer]): NodeSeq = {
    in.flatMap {r =>
      <tr>{
        r.render.map {c => <td>{c}</td>}
        }</tr>
    }
  }
}
