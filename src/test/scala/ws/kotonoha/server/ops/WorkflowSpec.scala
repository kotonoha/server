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

package ws.kotonoha.server.ops

import ws.kotonoha.model.WordStatus
import ws.kotonoha.server.actors.AkkaFun
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.records.{ExampleRecord, WordRecord}

/**
  * @author eiennohito
  * @since 2016/08/11
  */
class WorkflowSpec extends AkkaFun {
  def createWord(implicit ucxt: UserContext) = {
    val rec = WordRecord.createRecord
    val ex = ExampleRecord.createRecord.example("この例はどうにもならんぞ")
    ex.translation("This example is piece of shit!")
    rec.writing("例").reading("れい").meaning("example")
    rec.user(ucxt.uid).examples(List(ex))
    rec.status(WordStatus.Approved)
    rec
  }

  def saveWordAsync(implicit uctx: UserContext) = {
    val ops = uctx.inst[WordOps]
    ops.register(createWord, WordStatus.New)
  }
}
