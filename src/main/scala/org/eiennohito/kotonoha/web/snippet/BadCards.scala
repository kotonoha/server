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

package org.eiennohito.kotonoha.web.snippet

import xml.NodeSeq
import org.eiennohito.kotonoha.actors.ioc.{Akka, ReleaseAkka}
import org.eiennohito.kotonoha.records.{WordRecord, UserRecord}
import org.eiennohito.kotonoha.actors.learning.{WordsAndCards, LoadReviewList}
import akka.dispatch.Await
import net.liftweb.util.CssSel

/**
 * @author eiennohito
 * @since 27.06.12
 */

object BadCards extends Akka with ReleaseAkka  {
  import net.liftweb.util.Helpers._
  import akka.util.duration._

  def render(in: NodeSeq): NodeSeq = {
    val uid = UserRecord.currentId.openTheBox
    val obj = (akkaServ ? LoadReviewList(uid, 20)).mapTo[WordsAndCards]
    val res = Await.result(obj, 5.0 seconds)

    def tf(w: WordRecord) = {
      val css: CssSel = (".data *" #>
        <div>{w.writing.is}</div>
        <div>{w.reading.is}</div>) &
      ".mean *" #> <span>{w.meaning.is}</span> &
      ".sod *" #> <span>{Kakijyun.sod150(w.writing.is)}</span>
      css(in)
    }

    res.words.flatMap(w => tf(w))
  }
}
