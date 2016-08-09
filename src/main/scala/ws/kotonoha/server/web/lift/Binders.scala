/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.web.lift

import net.liftweb.common.Full
import net.liftweb.record.Field
import net.liftweb.util.{CanBind, HtmlHelpers}
import ws.kotonoha.server.util.NodeSeqUtil

import scala.xml.NodeSeq

/**
  * @author eiennohito
  * @since 2016/08/08
  */
object Binders extends HtmlHelpers {
  implicit def field2Bind[T](implicit render: Render[T]): CanBind[Field[T, _]] = new CanBind[Field[T, _]] {
    override def apply(it: => Field[T, _])(ns: NodeSeq) = it.valueBox match {
      case Full(d) => render.render(d)
      case _ => NodeSeq.Empty
    }
  }

  implicit def seq2Bind[T, C <: Iterable[Field[T, _]]](implicit render: Render[T]): CanBind[C] = new CanBind[C] {
    override def apply(it: => C)(ns: NodeSeq) = ???
  }

  def bseq[T](nseq: NodeSeq, s: Iterable[T], fn: T => NodeSeq => NodeSeq): NodeSeq = {
    if (s.isEmpty) return NodeSeq.Empty

    val iter = s.iterator
    val bldr = NodeSeq.newBuilder
    while (iter.hasNext) {
      val i = iter.next()
      bldr ++= fn(i)(nseq)
    }

    bldr.result()
  }

  def bseq[T](s: Iterable[T])(fn: T => NodeSeq => NodeSeq): NodeSeqFn = {
    (nseq: NodeSeq) => bseq(nseq, s, fn)
  }

  type NodeSeqFn = NodeSeqUtil.NodeSeqFn
}
