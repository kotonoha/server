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

package ws.kotonoha.server.test

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.google.inject.{Guice, Module, Provides, Scopes}
import com.typesafe.config.{Config, ConfigFactory}
import net.codingwell.scalaguice.ScalaModule
import org.bson.types.ObjectId
import ws.kotonoha.akane.config.Configuration
import ws.kotonoha.dict.jmdict.LuceneJmdict
import ws.kotonoha.server.KotonohaConfig
import ws.kotonoha.server.actors._
import ws.kotonoha.server.dict.{EmptyJmdict, JmdictService, JmdictServiceImpl}
import ws.kotonoha.server.ioc._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * @author eiennohito
 * @since 08.01.13 
 */

class TestModule(cfg: Config) extends ScalaModule {
  override def configure() = {
    bind[Res].toInstance(new ResourceManager)
    bind[Config].toInstance(cfg)
    bind[JmdictService].to[JmdictServiceImpl].in(Scopes.SINGLETON)
  }

  @Provides
  def jmdict(js: JmdictService): LuceneJmdict = {
    val instance = js.get()
    instance match {
      case EmptyJmdict =>
        js.asInstanceOf[JmdictServiceImpl].maybeUpdateJmdict()
        js.get()
      case _ => instance
    }
  }
}

object KotonohaTestAkka {
  val counter = new AtomicInteger(0)

  val cfg = KotonohaConfig.config

  def modules = Seq[Module](
    new TestModule(cfg),
    new AkkaModule("kt" + counter.getAndIncrement()),
    new GlobalActorsModule,
    new UserContextModule,
    new RMongoModule
  )
}

class KotonohaTestAkka extends AkkaMain {
  val inj = Guice.createInjector(KotonohaTestAkka.modules: _*)
  def ioc = inj.getInstance(classOf[IocActors])

  def system: ActorSystem = inj.getInstance(classOf[ActorSystem])
  override def global = inj.getInstance(classOf[GlobalActors]).global

  def userContext(uid: ObjectId) = new UserTestContext(this, uid, ioc.inst[UserContextService].of(uid))

  private[test] val cnt = new AtomicInteger()
}

class SupervisorActor extends UserScopedActor {
  override def receive = {
    case Nil => //
  }
}

class UserTestContext(akka: KotonohaTestAkka, val uid: ObjectId, val ctx: UserContext) {
  private implicit val timeout: Timeout = 10 minutes
  private implicit def system = akka.system
  def actor = ctx.actor

  private lazy val supervisor = {
    ctx.refFactory.actorOf(ctx.props[SupervisorActor], s"sv${akka.cnt.getAndAdd(1)}")
  }

  def userActor(props: Props, name: String) = {
    TestActorRef(props, supervisor, name)
  }

  def userActor[T <: UserScopedActor](name: String, tf: Props => Props = identity)(implicit ct: Manifest[T]) = {
    val props = ctx.props[T]
    TestActorRef.apply[T](tf(props), supervisor, name)
  }

  def svcActor(props: Props, name: String) = {
    TestActorRef(props, akka.global, name)
  }
}
