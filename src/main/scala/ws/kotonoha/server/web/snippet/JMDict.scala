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

import com.google.inject.Inject
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.http.{DispatchSnippet, S}
import net.liftweb.util.Props
import net.liftweb.util.Props.RunModes
import org.apache.lucene.search.Explanation
import ws.kotonoha.akane.dic.jmdict._
import ws.kotonoha.akane.dic.lucene.jmdict.LuceneJmdict
import ws.kotonoha.akane.dic.lucene.jmdict.JmdictQuery
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.util.LangUtil

import scala.xml.{NodeSeq, Text}

/**
 * @author eiennohito
 * @since 23.04.12
 */

class JMDict @Inject() (jmd: LuceneJmdict, uc: UserContext) extends DispatchSnippet with Logging {
  import net.liftweb.util.Helpers._
  import ws.kotonoha.server.util.NodeSeqUtil._

  override def dispatch = {
    case "fld" => fld
    case "list" => list
  }

  private val queryString = S.param("query").openOr("")
  private val explainQuery = RunModes.Development == Props.mode

  val fld: NodeSeqFn = {
    ".query-string [value]" #> queryString
  }

  def renderR(in: Seq[ReadingInfo], sep: String): NodeSeq = {
    val seq = transSeq(in, Text(sep)) { x =>
      val annots = annot(x.info)
      val prio = JMDictUtil.calculatePriority(x)
      <span><span class={s"dict-rw dict-prio-$prio"}>{x.content}</span><span class="dict-rd-tag">{annots}</span></span>
    }
    if (seq.isEmpty) seq else <span>【{seq}】</span>
  }

  def renderW(in: Seq[KanjiInfo], sep: String): NodeSeq = {
    val seq = transSeq(in, Text(sep)) { x =>
      val annots = annot(x.info)
      val prio = JMDictUtil.calculatePriority(x)
      <span><span class={s"dict-rw dict-prio-$prio"}>{x.content}</span><span class="dict-rd-tag">{annots}</span></span>
    }
    seq
  }

  def rendMeaning(m: MeaningInfo): NodeSeq = {
    val pos: NodeSeq = {
      val tmp = annot(m.pos ++ m.info)
      if (tmp.isEmpty) tmp
      else Text("(") ++ <span class="dict-mean-tag">{tmp}</span> ++ Text(") ")
    }
    val bdy = m.content.map(l => l.str).mkString("; ")
    pos ++ <span class="dict-mean">{bdy}</span>
  }


  def annot(m: Seq[JmdictTag]): NodeSeq = {
    transSeq(m, Text(", ")) { s =>
      val obj = JmdictTagMap.tagInfo(s.value)
      <span title={obj.explanation}>{obj.repr}</span>
    }
  }

  def processMeanings(ms: Seq[MeaningInfo]): NodeSeq = {
    if (ms.length == 1) {
      <div>{rendMeaning(ms.head)}</div>
    } else {
      val inner = ms.flatMap(m => <li>{rendMeaning(m)}</li>)
      <ol class="m-ent">{inner}</ol>
    }
  }

  def renderMeToo(rec: JmdictEntry) = {
    val w = rec.writings.view.map(_.content).headOption.getOrElse("")
    val r = rec.readings.view.map(_.content).headOption
    if (r.isDefined) {
      val cl = s"lift:ThisToo?wr=$w&rd=${r.get}&src=jmdict"
      <div class={cl}></div>
    } else if (w != "") {
      val cl = s"lift:ThisToo?wr=$w&src=jmdict"
      <div class={cl}></div>
    } else NodeSeq.Empty
  }

  private def parseQuery(qs: String): JmdictQuery = {
    JmdictQuery.fromString(qs, limit = 50, explain = explainQuery)
  }

  private val qobj = parseQuery(queryString)

  def debug(id: Long, expls: Map[Long, Explanation]): NodeSeq = {
    def renderExpl(e: Explanation): NodeSeq = {

      val children = e.getDetails.map(renderExpl).map(e => <li>{e}</li>)
      val stand = <div>{if (e.isMatch) "+" else "-"} {e.getValue} {e.getDescription}</div>

      stand ++ (if (children.isEmpty) Nil else <ul>{children}</ul>)
    }

    if (explainQuery) {
      expls.get(id) match {
        case Some(s) =>
          <div class="query-explanation">{renderExpl(s)}</div>
        case None => Nil
      }
    } else Nil
  }

  private val availableLangs = LangUtil.acceptableFor(uc.settings)

  def list(in: NodeSeq): NodeSeq = {
    val results = jmd.find(qobj)
    val fn = ".dict-entry *" #> results.data.map { o =>
      ".writing *" #> renderW(o.writings, ", ") &
      ".reading *" #> renderR(o.readings, ", ") &
      ".meanings *" #> processMeanings(JMDictUtil.cleanMeanings(o.meanings, availableLangs)) &
      ".me-too" #> renderMeToo(o) &
      ".debug *" #> debug(o.id, results.expls)
    } &
    ".total" #> results.totalHits &
    ".found" #> results.data.length &
    ".query" #> queryString

    fn apply in
  }
}
