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

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject._
import net.codingwell.scalaguice.ScalaModule
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.{ForUser, GlobalActors}

import scala.compat.java8.functionConverterImpls.AsJavaFunction
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/08/02
  */
class UserContext(val uid: ObjectId, gact: GlobalActors, injector: Injector) extends IocActorsImpl(injector) {

  import akka.pattern.ask

  import scala.concurrent.duration._

  def askUser[T: ClassTag](msg: AnyRef, timeout: Timeout = 2.seconds): Future[T] = {
    val future = gact.users.ask(ForUser(uid, msg))(timeout)
    future.mapTo[T]
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[UserContext]

  override def equals(other: Any): Boolean = other match {
    case that: UserContext =>
      (that canEqual this) &&
        uid == that.uid
    case _ => false
  }

  override def hashCode(): Int = {
    uid.hashCode()
  }
}


class UserContextInternalModule(uid: ObjectId) extends ScalaModule {
  override def configure() = {}

  @Singleton
  @Provides
  def userCtx(
    inj: Injector,
    gact: GlobalActors
  ): UserContext = new UserContext(uid, gact, inj)
}

abstract class IopProvisionException extends RuntimeException()

case class UserNotExistsException() extends IopProvisionException

trait UserContextService {
  def of(uid: ObjectId): UserContext
}

class UserContextServiceImpl @Inject() (
  inj: Injector,
  mods: Seq[Module]
) extends UserContextService {

  private val loader = new AsJavaFunction[ObjectId, UserContext](key => {
    val module = new UserContextInternalModule(key)
    val allModules = mods :+ module
    val childInj = inj.createChildInjector(allModules :_*)
    childInj.getInstance(classOf[UserContext])
  })

  private val cache = {
    Caffeine.newBuilder()
        .expireAfterAccess(20, TimeUnit.MINUTES)
        .build[ObjectId, UserContext]()
  }

  override def of(uid: ObjectId) = cache.get(uid, loader)
}

class UserContextModule extends ScalaModule {
  override def configure() = {}

  def modules: Seq[Module] = Nil

  @Provides
  @Singleton
  def ucxsvc(
    i: Injector
  ): UserContextService = new UserContextServiceImpl(i, modules)
}
