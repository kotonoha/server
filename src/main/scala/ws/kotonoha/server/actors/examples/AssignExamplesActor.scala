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

package ws.kotonoha.server.actors.examples

import akka.Done
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Cancellable}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.google.inject.Inject
import com.typesafe.config.Config
import org.joda.time
import org.joda.time.DateTime
import ws.kotonoha.server.ioc.IocActors

import scala.concurrent.ExecutionContext


/**
  * @author eiennohito
  * @since 2016/08/23
  */
class AssignExamplesActor @Inject() (
  aec: AssignExamplesConfig,
  cfg: Config,
  ioc: IocActors
)(implicit ec: ExecutionContext, amat: ActorMaterializer) extends Actor with ActorLogging {
  import AssignExamplesActor._

  var cancellable: Cancellable = null

  override def postStop(): Unit = {
    super.postStop()
    if (cancellable != null) {
      cancellable.cancel()
    }
  }


  import scala.concurrent.duration._
  override def preStart(): Unit = {
    if (aec.enabled && cfg.hasPath("examples.uri")) {
      log.info("automatic example assignment is ENABLED")
      cancellable = context.system.scheduler.scheduleOnce(5.seconds, self, DoAssign)
    } else {
      log.info("automatic example assignment is DISABLED")
      context.stop(self)
    }
  }

  var running = false
  var start = DateTime.now()
  var cnt = 0

  override def receive = {
    case DoAssign =>
      if (!running) {
        running = true
        cnt = 0
        start = DateTime.now()
        val assignment = ioc.inst[ExampleAssignment]
        val input = assignment.input
        val flow = assignment.mainFlow
        input.via(flow).runWith(Sink.actorRef(self, Done))
      }
    case s: ExampleAssignmentStatus =>
      log.debug(s"assigned ${s.number} exs for ${s.wid} in ${s.duration}")
      cnt += (if (s.number > 0) 1 else 0)
    case Done =>
      running = false
      log.info("updated {} words in {}", cnt, new time.Duration(start, DateTime.now()))
      cancellable = context.system.scheduler.scheduleOnce(24.hours, self, DoAssign)
    case Failure(e) =>
      running = false
      log.error(e, "error in getting example sentences for words")
      cancellable = context.system.scheduler.scheduleOnce(1.hour, self, DoAssign)
  }
}

object AssignExamplesActor {
  case object DoAssign
}
