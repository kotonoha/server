package ws.kotonoha.server.web.snippet

import net.liftweb._
import http._
import js._
import JsCmds._
import xml.NodeSeq
import ws.kotonoha.server.records.{UserRecord, WordCardRecord}
import ws.kotonoha.server.util.DateTimeUtils
import ws.kotonoha.server.util.Snippets._
import ws.kotonoha.server.web.ajax.AllJsonHandler

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
/**
 * @author eiennohito
 * @since 03.03.12
 */


object ScheduledCount {

  val callbackName = "loadDates"

  def script(in: NodeSeq): NodeSeq =
    Script(AllJsonHandler.is.jsCmd & callbackFn(callbackName))

  def cntScheduled(in: NodeSeq) : NodeSeq = {
    import com.foursquare.rogue.Rogue._
    val uid = UserRecord.currentId
    val now = DateTimeUtils.now

    val q = WordCardRecord where(_.user eqs uid.open_!) and (_.notBefore before now) and
      (_.learning.subfield(_.intervalEnd) before now)

    val cnt = q.count()
    if (cnt == 1) {
      <em>one card</em>
    } else {
      <em>{cnt} cards</em>
    }
  }
}
