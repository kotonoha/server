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

import xml.NodeSeq
import ws.kotonoha.server.actors.ioc.{Akka, ReleaseAkka}
import ws.kotonoha.server.records.{UserSettings, WordRecord, UserRecord}
import net.liftweb.http._
import ws.kotonoha.server.util.unapply.XInt
import ws.kotonoha.akane.unicode.UnicodeUtil
import ws.kotonoha.server.util.StrokeType
import collection.mutable.ListBuffer
import akka.actor.ActorRef
import ws.kotonoha.server.actors.UpdateRecord
import net.liftweb.common.Full
import scala.Some
import ws.kotonoha.server.actors.learning.LoadReviewList
import ws.kotonoha.server.actors.learning.WordsAndCards
import ws.kotonoha.server.web.rest.EmptyUserException
import akka.util.Timeout
import com.typesafe.scalalogging.{StrictLogging => Logging}
import concurrent.{Await, Promise, Future}

/**
 * @author eiennohito
 * @since 27.06.12
 */

trait UserActor extends Logging { self: Akka =>
  import akka.pattern.ask
  import concurrent.duration._
  implicit val ec = akkaServ.context

  implicit val timeout: Timeout = 10 seconds

  def userActor = create

  private def create: Future[ActorRef] = {
    UserRecord.currentId match {
      case Full(as) => akkaServ.userActorF(as)
      case _ => Promise[ActorRef].failure(new EmptyUserException).future
    }
  }

  def userAsk[T](msg: AnyRef)(implicit m: Manifest[T]): Future[T] = {
    userActor.flatMap { ar => ar ? msg}.mapTo[T]
  }

  def userTell(msg: AnyRef): Unit = {
    userActor.foreach( _ ! msg )
  }
}

object BadCards extends DispatchSnippet with Akka with ReleaseAkka with UserActor  {
  import net.liftweb.util.Helpers._
  import concurrent.duration._

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
    if (s == stripped.get) in else Nil
  }

  def selector(in: NodeSeq): NodeSeq = {
    val lst = (maxRecs.get :: List(10, 20, 30, 40, 50)).distinct.sorted
    val ents = lst map {_.toString} map {c => c -> c}
    def onSubmit(cnt: String): Unit = {
      cnt match {
        case XInt(c1) => {
          val c = c1 min 75
          val cur = UserSettings.current.badCount(c)
          userTell(UpdateRecord(cur))
          maxRecs.set(c)
        }
        case _ => ///
      }
    }
    val ns = SHtml.untrustedSelect(ents, Full(maxRecs.get.toString), onSubmit)
    ("select" #> ns).apply(in)
  }

  def surround(in: NodeSeq): NodeSeq = {
    if (stripped.get) { in }
    else { <lift:surround with="default" at="content">{in}</lift:surround> }
  }

  def sod(in: String) = {
    Kakijyun.japSod(in, sodType)
  }

  def kanjSod(in: String) = {
    Kakijyun.kanjiSod(in, sodType)
  }

  def sodType = {
    if (stripped.get) StrokeType.Png500
    else StrokeType.Png150
  }

  def words(in: NodeSeq): NodeSeq = {
    val uid = UserRecord.currentId.openTheBox
    val obj = userAsk[WordsAndCards](LoadReviewList(maxRecs.get))
    val res = Await.result(obj, 5.0 seconds)

    val wds = asRows(res.words)
    val ns = rowsToTable(wds)

    ("table *" #> ns).apply(in)
  }

  trait CellRenderer {
    def weight: Int = 1

    def render: NodeSeq
  }



  class VerySmallCell(w: WordRecord, len: Int) extends CellRenderer {
    override def weight = -100 * len - w.meaning.get.length

    def render = {
      val cl = "wc t%d".format(len)
      <div class={cl}>
        <div class="rd">{w.reading.get}</div>
        <div class="sod">{sod(w.writing.stris)}</div>
        <div class="mn">{w.meaning.get}</div>
      </div>
    }
  }


  class SimpleWordCell(w: WordRecord) extends CellRenderer {
    def render =
      <div class="wc half">
        <div class="rd">{w.reading.get}</div>
        <div class="sod">{sod(w.writing.stris)}</div>
        <div class="mn">{w.meaning.get}</div>
      </div>

    override def weight = w.meaning.get.length
  }

  class SimpleWordRow(w: WordRecord) extends CellRenderer {

    def left =
      <div>
        <div>
          <span class="wr">{w.writing.get}</span>
          <span class="rd">{w.reading.get}</span>
        </div>
        <span class="sod">{kanjSod(w.writing.stris)}</span>
      </div>


    override def weight = 50000

    def right = <div class="ma"><span class="mn">{w.meaning.get}</span></div>

    def render = <div class="wc row">{left ++ right}</div>
  }

  class LotsKanjiSmallMeaning(w: WordRecord, kanji: Int) extends CellRenderer {
    def left = {
      val style = "tc k%d".format(kanji)
      <div class={style}>
        <div>
          <span class="wr">{w.writing.get}</span>
          <span class="rd">{w.reading.get}</span>
        </div>
        <span class="sod">{kanjSod(w.writing.stris)}</span>
      </div>
    }

    def right = <div class="tc ma"><span class="mn">{w.meaning.get}</span></div>

    def render = <div class="wc tr">{left ++ right}</div>

    override def weight = 100000
  }

  object VerySmall {
    def unapply(w: WordRecord): Option[CellRenderer] = {
      val l1 = w.writing.stris.length
      val l2 = w.meaning.get.length

      if (l1 < 4 && (25 + l1 * 25) > l2) {
        Some(new VerySmallCell(w, l1))
      } else None
    }
  }

  object SimpleSmall {
    def unapply(w: WordRecord): Option[SimpleWordCell] = {
      val b1 = w.meaning.get.length < 300
      val b2 = w.writing.stris.length < 6
      if (b1 && b2) {
        Some(new SimpleWordCell(w))
      } else None
    }
  }

  object LotsOfKanjiSmallMean {
    def unapply(w: WordRecord): Option[LotsKanjiSmallMeaning] = {
      val l1 = w.meaning.get.length
      val l2 = UnicodeUtil.klen(w.writing.stris)
      if ((l2 * 120 + l1) < 1100) Some(new LotsKanjiSmallMeaning(w, l2)) else None
    }
  }

  def asRows(in: List[WordRecord]) = {
    val cells = new ListBuffer[CellRenderer]
    in.foreach {
      case VerySmall(c) if (stripped.get) => cells += c
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
