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
import org.apache.lucene.search.BooleanClause.Occur
import ws.kotonoha.akane.dic.jmdict._
import ws.kotonoha.dict.jmdict.{JmdictQuery, JmdictQueryPart, LuceneJmdict}
import ws.kotonoha.server.records.dictionary.JMDictRecord
import ws.kotonoha.server.util.LangUtil

import scala.collection.mutable.ArrayBuffer
import scala.xml.{NodeSeq, Text}

/**
 * @author eiennohito
 * @since 23.04.12
 */

class JMDict @Inject() (jmd: LuceneJmdict) extends DispatchSnippet with Logging {
  import net.liftweb.util.Helpers._
  import ws.kotonoha.server.util.NodeSeqUtil._


  override def dispatch = {
    case "fld" => fld
    case "list" => list
  }

  def fld(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    bind("frm", in, AttrBindParam("value", q, "value"))
  }

  def renderR(in: Seq[ReadingInfo], sep: String): NodeSeq = {
    transSeq(in, Text(sep)) { x =>
      val annots = annot(x.info)
      val prio = JMDictRecord.calculatePriority(x)
      <span><span class={s"dict-rw dict-prio-$prio"}>{x.content}</span><span class="dict-rd-tag">{annots}</span></span>
    }
  }

  def renderW(in: Seq[KanjiInfo], sep: String): NodeSeq = {
    transSeq(in, Text(sep)) { x =>
      val annots = annot(x.info)
      val prio = JMDictRecord.calculatePriority(x)
      <span><span class={s"dict-rw dict-prio-$prio"}>{x.content}</span><span class="dict-rd-tag">{annots}</span></span>
    }
  }

  def rendMeaning(m: MeaningInfo): NodeSeq = {
    val pos: NodeSeq = {
      val tmp = annot(m.pos ++ m.info)
      if (tmp.isEmpty) tmp
      else Text("(") ++ <span class="dict-mean-tag">{tmp}</span> ++ Text(") ")
    }
    val bdy = m.content.filter(l => LangUtil.okayLang(l.lang)).map(l => l.str).mkString("; ")
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
    val parts = qs.split("\\s+").filter(_.length > 0)

    val rds = new ArrayBuffer[JmdictQueryPart]()
    val wrs = new ArrayBuffer[JmdictQueryPart]()
    val tags = new ArrayBuffer[JmdictQueryPart]()
    val other = new ArrayBuffer[JmdictQueryPart]()


    parts.foreach { p =>

      val (occur, next) = p.charAt(0) match {
        case '+' => (Occur.MUST, p.substring(1))
        case '-' => (Occur.MUST_NOT, p.substring(1))
        case _ => (Occur.SHOULD, p)
      }

      next.split(":") match {
        case Array(pref, suf) =>
          val part = JmdictQueryPart(suf, occur)
          val coll = pref match {
            case s if s.startsWith("w") => wrs
            case s if s.startsWith("r") => rds
            case s if s.startsWith("t") => tags
            case _ => other
          }
          coll += part
        case _ =>
          other += JmdictQueryPart(next)
      }
    }
    JmdictQuery(
      limit = 50,
      rds, wrs, tags, other
    )
  }

  def list(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    val qobj = parseQuery(q)
    val results = jmd.find(qobj)
    results flatMap { o =>
      bind("je", in,
        "writing" -> renderW(o.writings, ", "),
        "reading" -> renderR(o.readings, ", "),
        "metoo" -> renderMeToo(o),
        "body" -> processMeanings(o.meanings)
      )
    }
  }
}
