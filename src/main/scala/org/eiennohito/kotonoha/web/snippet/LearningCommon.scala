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

import net.liftweb.common.Full
import xml.{Text, NodeSeq}
import org.eiennohito.kotonoha.records.{WordCardRecord, UserRecord}
import org.eiennohito.kotonoha.util.DateTimeUtils

/**
 * @author eiennohito
 * @since 04.04.12
 */

object LearningCommon {
  import net.liftweb.util.Helpers._
  import com.foursquare.rogue.Rogue._

  def stats(in: NodeSeq):NodeSeq = {
    val uid = UserRecord.currentId
    uid match {
      case Full(id) => {
        val q = WordCardRecord where (_.user eqs id)
        val total = q.count()
        val lq = q and (_.learning exists (true))
        val learning = lq count()
        val nsq = q and (_.learning exists (false))
        val ns = nsq.count()
        val now = DateTimeUtils.now
        val nsf = nsq and (_.notBefore after now) count()
        val sf = lq and (_.notBefore after now) raw (f => f.add("$where", "this.notBefore < this.learning.intervalEnd")) count()

        val av_1 = lq where (_.learning subfield(_.intervalEnd) before now) and (_.notBefore before now) count()
        val av_2 = nsq where (_.notBefore before now) count()
        bind("st", in,
          "total" -> total,
          "learning" -> learning,
          "ns" -> ns,
          "nsf" -> nsf,
          "sf" -> sf,
          "available" -> (av_1 + av_2))
      }
      case _ => Text("Please login or register")
    }


  }

}
