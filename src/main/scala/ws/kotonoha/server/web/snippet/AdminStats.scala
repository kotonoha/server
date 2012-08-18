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
import ws.kotonoha.server.util.{Formatting, DateTimeUtils}

/**
 * @author eiennohito
 * @since 21.07.12
 */

object AdminStats {

  def d(i: Int) = Formatting.monthDay(DateTimeUtils.now.minusDays(i))

  def learningHead(in: NodeSeq) = {
    val dates = (Range.inclusive(10, 2, -1) map d).toList
    val cols = ("Name" :: dates) :::  List("Yesterday", "Today")
    cols flatMap {c => <td>{c}</td>}
  }
}
