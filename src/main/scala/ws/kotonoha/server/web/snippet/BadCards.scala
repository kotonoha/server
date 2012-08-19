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

import xml.{Text, NodeSeq}
import ws.kotonoha.server.actors.ioc.{Akka, ReleaseAkka}
import ws.kotonoha.server.records.{UserSettings, WordRecord, UserRecord}
import ws.kotonoha.server.actors.learning.{WordsAndCards, LoadReviewList}
import akka.dispatch.Await
import net.liftweb.util.CssSel
import net.liftweb.http.{SHtml, RequestVar, S, DispatchSnippet}
import net.liftweb.common.Full
import ws.kotonoha.server.util.unapply.XInt
import ws.kotonoha.server.actors.{UpdateRecord, SaveRecord}
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.server.util.StrokeType
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

    val wds = asRows(res.words)
    val ns = rowsToTable(wds)

    ("table *" #> ns) (in)
  }

  trait CellRenderer {
    def weight: Int = 1

    def render: NodeSeq
  }



  class VerySmallCell(w: WordRecord, len: Int) extends CellRenderer {
    override def weight = -100 * len - w.meaning.is.length

    def render = {
      val cl = "wc t%d".format(len)
      <div class={cl}>
        <div class="rd">{w.reading.is}</div>
        <div class="sod">{sod(w.writing.is)}</div>
        <div class="mn">{w.meaning.is}</div>
      </div>
    }
  }


  class SimpleWordCell(w: WordRecord) extends CellRenderer {
    def render =
      <div class="wc half">
        <div class="rd">{w.reading.is}</div>
        <div class="sod">{sod(w.writing.is)}</div>
        <div class="mn">{w.meaning.is}</div>
      </div>

    override def weight = w.meaning.is.length
  }

  class SimpleWordRow(w: WordRecord) extends CellRenderer {

    def left =
      <div>
        <div>
          <span class="wr">{w.writing.is}</span>
          <span class="rd">{w.reading.is}</span>
        </div>
        <span class="sod">{kanjSod(w.writing.is)}</span>
      </div>


    override def weight = 50000

    def right = <div class="ma"><span class="mn">{w.meaning.is}</span></div>

    def render = <div class="wc row">{left ++ right}</div>
  }

  class LotsKanjiSmallMeaning(w: WordRecord, kanji: Int) extends CellRenderer {
    def left = {
      val style = "tc k%d".format(kanji)
      <div class={style}>
        <div>
          <span class="wr">{w.writing.is}</span>
          <span class="rd">{w.reading.is}</span>
        </div>
        <span class="sod">{kanjSod(w.writing.is)}</span>
      </div>
    }

    def right = <div class="tc ma"><span class="mn">{w.meaning.is}</span></div>

    def render = <div class="wc tr">{left ++ right}</div>

    override def weight = 100000
  }

  object VerySmall {
    def unapply(w: WordRecord): Option[CellRenderer] = {
      val l1 = w.writing.is.length
      val l2 = w.meaning.is.length

      if (l1 < 4 && (25 + l1 * 25) > l2) {
        Some(new VerySmallCell(w, l1))
      } else None
    }
  }

  object SimpleSmall {
    def unapply(w: WordRecord): Option[SimpleWordCell] = {
      val b1 = w.meaning.is.length < 300
      val b2 = w.writing.is.length < 6
      if (b1 && b2) {
        Some(new SimpleWordCell(w))
      } else None
    }
  }

  object LotsOfKanjiSmallMean {
    def unapply(w: WordRecord): Option[LotsKanjiSmallMeaning] = {
      val l1 = w.meaning.is.length
      val l2 = UnicodeUtil.klen(w.writing.is)
      if ((l2 * 120 + l1) < 1100) Some(new LotsKanjiSmallMeaning(w, l2)) else None
    }
  }

  def asRows(in: List[WordRecord]) = {
    val cells = new ListBuffer[CellRenderer]
    in.foreach {
      case VerySmall(c) if (stripped.is) => cells += c
      case SimpleSmall(c) => cells += c
      case LotsOfKanjiSmallMean(c) => cells += c
      case w => cells += new SimpleWordRow(w)
    }
    cells.sortBy(-_.weight).toList
  }

  def rowsToTable(in: List[CellRenderer]): NodeSeq = {
    in.flatMap { _.render }
  }
}
