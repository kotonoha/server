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

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.mongodb.BasicDBObject
import org.joda.time.{DateTime, Duration => JDur}
import ws.kotonoha.server.test.AkkaFree
import ws.kotonoha.server.util.DateTimeUtils

/**
  * @author eiennohito
  * @since 2016/08/11
  */

object ReactiveRogueCleaner {
  private lazy val cleaned: Boolean = {
    ReactiveRecord.bulkDelete_!!(new BasicDBObject())
    true
  }

  def clean(): Unit = cleaned
}

class ReactiveRogueSpec extends AkkaFree with MongoAwareTest { test =>
  private [this] val acc = kta.ioc.inst[RMData]
  private val date = DateTimeUtils.now
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    ReactiveRogueCleaner.clean()
    createRecord(date)
  }

  def createRecord(dt: DateTime): Unit = {
    val rec = ReactiveRecord.createRecord
    rec.textfld("test").opttest(10).date(dt).save()
  }

  val rrogue: ReactiveRogue = new ReactiveRogue {
    override protected[mongodb] def collection(name: String) = acc.asInstanceOf[ReactiveOpsAccess].collection(name)
    override protected implicit def ec = test.kta.system.dispatcher
  }

  "ReactiveRogure" - {
    import KotonohaLiftRogue._

    "fetch works" in {
      val q = ReactiveRecord.where(_.date eqs date).limit(10)
      val res = rrogue.fetch(q)
      ares(res).loneElement.date.get shouldBe date
    }

    "fetch with projection works with datetime" in {
      val q = ReactiveRecord.where(_.date eqs date).select(_.date)
      val res = rrogue.fetch(q)
      ares(res).loneElement shouldBe date
    }

    "fetch with projection works with string" in {
      val q = ReactiveRecord.where(_.date eqs date).select(_.textfld)
      val res = rrogue.fetch(q)
      ares(res).loneElement shouldBe "test"
    }

    "fetch with projection works with optint" in {
      val q = ReactiveRecord.where(_.date eqs date).select(_.opttest)
      val res = rrogue.fetch(q)
      ares(res).loneElement shouldBe Some(10)
    }

    "fetch with projection works with optint2" in {
      val q = ReactiveRecord.where(_.date eqs date).select(_.opttest2, _.opttest)
      val res = rrogue.fetch(q)
      ares(res).loneElement shouldBe (None, Some(10))
    }

    implicit val amat = ActorMaterializer.create(kta.system)

    "stream works" in {
      val q = ReactiveRecord.where(_.date eqs date).limit(10)
      val str = rrogue.stream(q)
      val res = str.runWith(Sink.seq)
      ares(res).loneElement.date.get shouldBe date
    }

    "stream works with 300 items" in {
      val items = (1 to 300) map {i =>
        ReactiveRecord.createRecord.date(date.plusSeconds(i)).textfld("what")
      }
      ares(acc.save(items)).n shouldBe 300
      val q = ReactiveRecord.where(_.textfld eqs "what")
      val str = acc.stream(q)
      val f = str.runWith(Sink.fold(0){(i, _) => i + 1})
      ares(f) shouldBe 300
    }

    "stream works with 3k items and projection" in {
      val cnt = 3000
      val items = (1 to cnt) map {i =>
        ReactiveRecord.createRecord.date(date.plusSeconds(i)).textfld("what3k")
      }
      ares(acc.save(items)).n shouldBe cnt
      val q = ReactiveRecord.where(_.textfld eqs "what3k").select(_.date)
      val str = acc.stream(q, 99)
      val f = str.runWith(Sink.fold(0L){(i, d) => i + new JDur(date, d).getStandardSeconds})
      ares(f) shouldBe (cnt.toLong*(cnt + 1)/2)
    }

    "update works" in {
      val q = ReactiveRecord.where(_.date eqs date)
      val upd = q.modify(_.textfld.setTo("asdf"))
      ares(rrogue.update(upd)).nModified shouldBe 1
      val loaded = q.fetch()
      loaded.loneElement.textfld.get shouldBe "asdf"
    }

    "delete works" in {
      val d2 = date.plusDays(2)
      createRecord(d2)
      val q = ReactiveRecord.where(_.date.eqs(d2))
      ares(rrogue.remove(q)).n shouldBe 1
    }
  }
}
