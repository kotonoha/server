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

package ws.kotonoha.server.mongodb

import ws.kotonoha.server.test.AkkaFree
import ws.kotonoha.server.util.DateTimeUtils

/**
  * @author eiennohito
  * @since 2016/08/11
  */
class ReactiveRogureSpec extends AkkaFree { test =>
  private [this] val acc = kta.ioc.inst[RMData]
  private val date = DateTimeUtils.now
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val rec = ReactiveRecord.createRecord
    rec.textfld("test").date(date).save()
  }

  val rrogue: ReactiveRogue = new ReactiveRogue {
    override protected[mongodb] def collection(name: String) = acc.asInstanceOf[ReactiveOpsAccess].collection(name)
    override protected implicit def ec = test.kta.system.dispatcher
  }

  "ReactiveRogure" - {
    "works" in {
      import KotonohaLiftRogue._
      val q = ReactiveRecord.where(_.date eqs date).limit(10)
      val res = rrogue.fetch(q)
      ares(res).loneElement.date.get shouldBe date
    }
  }
}
