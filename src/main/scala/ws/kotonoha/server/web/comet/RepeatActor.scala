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

package ws.kotonoha.server.web.comet

import akka.actor.{PoisonPill, Scheduler}
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.http.ShutDown
import net.liftweb.http.js.JsCmds
import net.liftweb.json.JsonAST._
import net.liftweb.util.Helpers.TimeSpan
import org.bson.types.ObjectId
import ws.kotonoha.lift.json.{JFormat, JLCaseClass, JRead, JWrite}
import ws.kotonoha.model.CardMode
import ws.kotonoha.server.actors.AkkaMain
import ws.kotonoha.server.actors.learning.RepeatBackendActor
import ws.kotonoha.server.actors.lift.{AkkaInterop, NgLiftActor, Ping}
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.util.DateTimeUtils

import scala.concurrent.ExecutionContext

/**
  * @author eiennohito
  * @since 21.05.12
  */

object RepeatActor {

  import ws.kotonoha.server.actors.learning.RepeatBackend._

  implicit object cardModeStringWrite extends JWrite[CardMode] {
    override def write(o: CardMode): JValue = o match {
      case CardMode.Writing => JString("writing")
      case CardMode.Reading => JString("reading")
      case _ => JNothing
    }
  }

  implicit val rqpWrite = JLCaseClass.write[RepQuestionPart]
  implicit val reWrite = JLCaseClass.write[ReviewExample]
  implicit val readdWrite = JLCaseClass.write[RepAdditional]

  case class PublishCards(cards: Seq[RepCard])
  case class WebMsg(cmd: String, data: JValue)

  object WebMsg {
    def apply[T](cmd: String, o: T)(implicit write: JWrite[T]): WebMsg = WebMsg(cmd, write.write(o))
  }

  implicit val wmFormat = JLCaseClass.format[WebMsg]
  implicit val rcFormat = JLCaseClass.write[RepCount]

  implicit object oidRead extends JFormat[ObjectId] {
    override def read(v: JValue): Box[ObjectId] = v match {
      case JString(s) =>
        if (s.length == 24) try {
          Full(new ObjectId(s))
        } catch {
          case e: Exception => Failure(s"could not serialize objectId from $s", Full(e), Empty)
        } else {
          Failure(s"length of $s was not exactly 24")
        }
      case x =>
        Failure(s"when reading objectid expected JString, found $x")
    }

    override def write(o: ObjectId): JValue = {
      JString(o.toHexString)
    }
  }

  implicit val rcWrite = JLCaseClass.write[RepCard]
  implicit val webMarkRd = JLCaseClass.read[WebMark]
  implicit val markAddRead = JLCaseClass.read[MarkAddition]
  implicit val exReportRd = JLCaseClass.read[RepExReport]
}

class RepeatActor @Inject()(
  uc: UserContext,
  val akkaServ: AkkaMain
)(implicit ec: ExecutionContext, sch: Scheduler) extends AkkaInterop with NgLiftActor with StrictLogging {
  import DateTimeUtils._
  import RepeatActor._

  import concurrent.duration._

  private val backend = uc.refFactory.actorOf(uc.props[RepeatBackendActor])
  private def self = this

  private val cancellable = sch.schedule(5.minutes, 1.minute) {
    self ! Ping
  }

  private var last = now

  override def localShutdown() {
    cancellable.cancel()
    backend ! PoisonPill
    super.localShutdown()
  }

  override def svcName: String = "RepeatBackend"

  override def receiveJson = {
    case obj => obj \ "cmd" match {
      case JString("mark") =>
        webMarkRd.read(obj) match {
          case Full(m) => backend ! m
          case x => logger.error(s"could not read mark from json $obj: $x")
        }
      case JString("nextTime") =>
        markAddRead.read(obj) match {
          case Full(m) => backend ! m
          case x => logger.error(s"could not read mark addition from json $obj: $x")
        }
      case JString("report-ex") =>
        exReportRd.read(obj) match {
          case Full(m) => backend ! m
          case x => logger.error(s"could not read example report from json $obj: $x")
        }
      case _ => logger.warn(s"unknown json input $obj")
    }
    JsCmds.Noop
  }

  override protected def alwaysReRenderOnPageLoad: Boolean = true

  override protected def localSetup(): Unit = {
    super.localSetup()
    backend ! self
  }

  override def lowPriority = {
    case PublishCards(cards) =>
      self ! WebMsg("cards", cards)
    case msg: WebMsg =>
      ngMessage(msg)
      last = now
    case Ping =>
      val dur = new org.joda.time.Duration(last, now)
      if (dur.getStandardMinutes > 13) {
        ngMessage(WebMsg("timeout", JNothing))
        self ! ShutDown
      }
  }

  override def lifespan: Box[TimeSpan] = Full(15.minutes)
}
