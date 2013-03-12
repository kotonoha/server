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

import scala.xml.{Text, NodeSeq}
import net.liftweb.http.S
import ws.kotonoha.server.util.LangUtil
import ws.kotonoha.server.records.dictionary.{JMDictAnnotations, JMDictMeaning, JMString, JMDictRecord}
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author eiennohito
 * @since 23.04.12
 */

object JMDict extends Logging {
  import net.liftweb.util.Helpers._
  import ws.kotonoha.server.util.NodeSeqUtil._

  def fld(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    bind("frm", in, AttrBindParam("value", q, "value"))
  }

  def renderRW(in: List[JMString], sep: String): NodeSeq = {
    transSeq(in, Text(sep)) { x =>
      val annots = annot(x.info.is)
      val prio = JMDictRecord.calculatePriority(x :: Nil)
      <span><span class={s"dict-rw dict-prio-$prio"}>{x.value.is}</span><span class="dict-rd-tag">{annots}</span></span>
    }
  }

  def rendMeaning(m: JMDictMeaning): NodeSeq = {
    val pos: NodeSeq = {
      val tmp = annot(m.info.is)
      if (tmp.isEmpty) tmp
      else Text("(") ++ <span class="dict-mean-tag">{tmp}</span> ++ Text(") ")
    }
    val bdy = m.vals.is.filter(l => LangUtil.okayLang(l.loc)).map(l => l.str).mkString("; ")
    pos ++ <span class="dict-mean">{bdy}</span>
  }


  def annot(m: List[String]): NodeSeq = {
    transSeq(m, Text(", ")) { s =>
      val lng = JMDictAnnotations.safeValueOf(s).long
      <span title={lng}>{s}</span>
    }
  }

  def processMeanings(ms: List[JMDictMeaning]): NodeSeq = {
    ms.tail match {
      case Nil => <div>{rendMeaning(ms.head)}</div>
      case _ =>
        val inner = ms.flatMap(m => <li>{rendMeaning(m)}</li>)
        <ol class="m-ent">{inner}</ol>
    }
  }

  def list(in: NodeSeq): NodeSeq = {
    val q = S.param("query").openOr("")
    JMDictRecord.query(q, None, 50, true) flatMap { o =>
      bind("je", in,
        "writing" -> renderRW(o.writing.is, ", "),
        "reading" -> renderRW(o.reading.is, ", "),
        "body" -> processMeanings(o.meaning.is)
      )
    }
  }
}
