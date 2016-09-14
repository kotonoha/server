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

package ws.kotonoha.server.ioc

import com.google.inject._
import org.bson.types.ObjectId
import org.scalatest.{FreeSpec, Matchers}
import ws.kotonoha.server.actors.GlobalActorsModule
import ws.kotonoha.server.actors.examples.AssignExamplesModule
import ws.kotonoha.server.test.{KotonohaTestAkka, TestModule}

/**
  * @author eiennohito
  * @since 2016/08/11
  */
class UserContextSpec extends FreeSpec with Matchers {
  val modules = Seq[Module](
    new TestModule(KotonohaTestAkka.cfg),
    new AkkaModule("ucs"),
    new GlobalActorsModule,
    new UserContextModule,
    new AssignExamplesModule
  )

  val inj = Guice.createInjector(modules: _*)

  "UserContext" - {
    "works with two things" in {
      val ucs = inj.getInstance(classOf[UserContextService])
      val cx1 = ucs.of(ObjectId.get())
      val cx2 = ucs.of(ObjectId.get())
      cx1 shouldNot equal (cx2)
      val t1 = cx1.inst[Test]
      val t2 = cx2.inst[Test]
      t1 shouldNot equal (cx2)
      cx1.uid shouldBe t1.id
      cx2.uid shouldBe t2.id
    }

    "child should not fail even if parent fails" in {
      val ucs = inj.getInstance(classOf[UserContextService])
      val cx = ucs.of(ObjectId.get())
      an [ConfigurationException] shouldBe thrownBy (inj.getInstance(classOf[Test]))
      cx.inst[Test].id shouldBe cx.uid
    }
  }
}

@Singleton
private[ioc] class Test @Inject() (
  uc: UserContext
) {
  def id = uc.uid

  override def hashCode(): Int = id.hashCode()

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case t: Test => t.id == id
      case _ => false
    }
  }
}
