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

import akka.util.Timeout
import com.google.inject.Provides
import net.codingwell.scalaguice.ScalaModule
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.{ForUser, GlobalActors}
import ws.kotonoha.server.records.UserRecord

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/08/02
  */
class UserContext(val uid: ObjectId, gact: GlobalActors) {

  import akka.pattern.ask
  import scala.concurrent.duration._

  def askUser[T: ClassTag](msg: AnyRef, timeout: Timeout = 2.seconds): Future[T] = {
    val future = gact.users.ask(ForUser(uid, msg))(timeout)
    future.mapTo[T]
  }
}


class UserContextModule extends ScalaModule {
  override def configure() = {}

  @Provides
  def ctx(
    gact: GlobalActors
  ): UserContext = {
    new UserContext(
      UserRecord.currentId.openOr(throw new UserNotExistsException),
      gact
    )
  }
}

abstract class IopProvisionException extends RuntimeException()

case class UserNotExistsException() extends IopProvisionException
