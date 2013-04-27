/*
 * Copyright 2012-2013 eiennohito
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

package ws.kotonoha.server.wiki.template

/**
 * @author eiennohito
 * @since 20.04.13 
 */

object AddWord extends Template {
  def previewAddBtn = <button class="btn btn-mini"><i class="icon-plus"></i> add this</button>

  def apply(args: String) = {
    args.split(":") match {
      case Array(wr, rd) =>
        val btn = if (preview) previewAddBtn
        else <lift:ThisToo wr={wr} rd={rd} src="wiki" /> ;
        <span>
          <span class="nihongo">
            <ruby><rb>{wr}</rb><rt>{rd}</rt></ruby>
          </span>
          {btn}
        </span>
      case Array(wr) =>
        val btn = if (preview) previewAddBtn
        else <lift:ThisToo writing={wr} src="wiki" /> ;
        <span>
          <span class="nihongo">{wr}</span>{btn}
        </span>
      case _ => Nil
    }
  }
}

object AddWordBtn extends Template {
  import AddWord.previewAddBtn

  def apply(args: String) = args.split(":") match {
    case Array(wr, rd) =>
      if (preview) previewAddBtn
      else <lift:ThisToo wr={wr} rd={rd} src="wiki" />
    case Array(wr) =>
      if (preview) previewAddBtn
      else <lift:ThisToo writing={wr} src="wiki" />
    case _ => Nil
  }
}
