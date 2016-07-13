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

package ws.kotonoha.server.web.rest

import akka.util.Timeout
import com.typesafe.scalalogging.{StrictLogging => Logging}
import net.liftweb.common.{Full, _}
import net.liftweb.http._
import net.liftweb.http.rest.RestContinuation
import org.bson.types.ObjectId
import ws.kotonoha.server.actors.ioc.Akka
import ws.kotonoha.server.records.UserRecord

import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions
import scala.reflect.ClassTag

class EmptyUserException extends Exception("No user present")

trait KotonohaRest extends OauthRestHelper with Logging with Akka {
  import akka.pattern.ask

  import scala.concurrent.duration._

  lazy implicit val scheduler = akkaServ.context
  lazy implicit val timeout: Timeout = 20.seconds

  def userId = UserRecord.currentId

  def userAsk[T](uid: Box[ObjectId], msg: AnyRef)(implicit m: ClassTag[T]): Future[T] = userId match {
    case Full(xid) => userAsk[T](xid, msg)(m)
    case _ => Promise[T].failure(new EmptyUserException).future
  }

  def userAsk[T](msg: AnyRef)(implicit m: ClassTag[T]): Future[T] = userAsk[T](userId, msg)(m)

  def userAsk[T](uid: ObjectId, msg: AnyRef)(implicit m: ClassTag[T]): Future[T] = {
    akkaServ.userActorF(uid).flatMap {
      a => a ? msg
    }.mapTo[T]
  }

  def async[Obj, Res](param: Future[Obj])(f: (Obj => Future[Box[Res]]))(implicit rtf: Res => LiftResponse) = {
    RestContinuation.async({
      resp =>
        param onComplete {
          case scala.util.Failure(ex) => {
            logger.error("Error in getting parameter", ex)
            resp(PlainTextResponse("Internal server error", 500))
          }
          case scala.util.Success(v) => {
            val fut = f(v)
            val tCancel = akkaServ.schedule(() => resp(PlainTextResponse("Sevice timeouted", 500)), 20 seconds)

            fut onSuccess {
              case Full(r) => tCancel.cancel(); resp(r)
              case x@_ => logger.debug("found something: " + x)
            }
          }
        }
    })
  }

  def async[Obj, Res](param: Box[Obj])(f: (Obj => Future[Box[Res]]))(implicit rtf: Res => LiftResponse) = {
    RestContinuation.async({
      resp =>
        param match {
          case Empty => resp(PlainTextResponse("No response", 500))
          case b: EmptyBox => {
            emptyToResp(b) map (resp(_))
          }
          case Full(v) => {
            val fut = f(v)
            val tCancel = akkaServ.schedule(() => resp(PlainTextResponse("Sevice timeouted", 500)), 20 seconds)

            fut onComplete {
              case scala.util.Success(Full(r)) => tCancel.cancel(); resp(r)
              case scala.util.Failure(e) => logger.error("Error in executing rest:", e)
              case x@_ => logger.debug("found something: " + x)
            }
          }
        }
    })
  }

  override def needAuth = !(S.inStatefulScope_? && UserRecord.currentUserId.isDefined)

  class EmptyBoxException(msg: String = null, t: Throwable = null) extends RuntimeException(msg, t)

  def kept[T](obj: T)(implicit cvf: T => LiftResponse) = {
    Promise[Box[LiftResponse]].success(Full(obj))
  }

  implicit class WrappedBox[T](val b: Box[T]) {
    def fut: Future[T] = b match {
      case Full(x) => Future {
        x
      }
      case Empty => Future.failed(new EmptyBoxException)
      case Failure(msg, ex, _) => Future.failed(new EmptyBoxException(msg, ex.openOr(null)))
    }
  }

  implicit def option2WrappedBox[T](in: Option[T]): WrappedBox[T] = new WrappedBox[T](in)
}
