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

package ws.kotonoha.server.util

import akka.actor.ActorRef
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, ZipWith}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage._
import com.typesafe.scalalogging.StrictLogging
import ws.kotonoha.akane.akka.MaxAtOnceActor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/08/22
  */
class RateLimiterStage[T: ClassTag](hndl: ActorRef) extends GraphStageWithMaterializedValue[FlowShape[T, T], Future[ActorRef]] with StrictLogging {
  val in = Inlet[T]("grls.in")
  val out = Outlet[T]("grls.out")
  val shape = FlowShape(in, out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    val aref = Promise[ActorRef]
    val logic = new GraphStageLogic(shape) with InHandler with OutHandler {
      setHandlers(in, out, this)

      private var finished = false
      private var waiting = false
      private var ref: ActorRef = null
      override def preStart(): Unit = {
        val actor = getStageActor {
          case (_, MaxAtOnceActor.Acknowledge(p)) =>
            push(out, p.asInstanceOf[T])
            if (finished) {
              completeStage()
            } else {
              waiting = false
            }
        }
        ref = actor.ref
        aref.success(ref)
      }

      override def onPush() = {
        val obj = grab(in)
        hndl.tell(MaxAtOnceActor.Request(obj), ref)
        waiting = true
      }

      override def onPull() = {
        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        finished = true
        if (!waiting) {
          completeStage()
        }
      }
    }
    (logic, aref.future)
  }
}

final class HoldWithWait[T](indefinitiely: Boolean = true) extends GraphStage[FlowShape[T, T]] {
  val in = Inlet[T]("HoldWithWait.in")
  val out = Outlet[T]("HoldWithWait.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var currentValue: T = _
    private var waitingFirstValue = true

    setHandlers(in, out, new InHandler with OutHandler {
      override def onPush(): Unit = {
        currentValue = grab(in)
        if (waitingFirstValue) {
          waitingFirstValue = false
          if (isAvailable(out)) push(out, currentValue)
        }
        pull(in)
      }

      override def onPull(): Unit = {
        if (!waitingFirstValue) push(out, currentValue)
      }

      override def onUpstreamFinish(): Unit = {
        if (!indefinitiely) {
          completeStage()
        }
      }
    })

    override def preStart(): Unit = {
      pull(in)
    }
  }
}

trait RateLimiter {
  def limit[I: ClassTag, O, M](proc: Flow[I, O, M]): Flow[I, O, M]
}

object GlobalRateLimiting {
  def limit[I: ClassTag, O, M](aref: ActorRef, proc: Flow[I, O, M])(implicit ec: ExecutionContext): Flow[I, O, M] = {
    import GraphDSL.Implicits._
    val g = GraphDSL.create(proc, new RateLimiterStage[I](aref)) {(a, b) => (a, b)} { implicit b => (p, lim) =>
      val split = b.add(new Broadcast[I](2, eagerCancel = false))
      lim.out ~> split
      split ~> p

      val mv = b.materializedValue.map(_._2)
      val duplicator = b.add(new HoldWithWait[Future[ActorRef]])

      val zip = b.add(ZipWith((t: I, o: O, ar: Future[ActorRef]) => {
        ar.foreach(a => aref.tell(MaxAtOnceActor.Finished(t), a))
        o
      }))

      split ~> zip.in0
      p ~> zip.in1
      mv ~> duplicator ~> zip.in2

      FlowShape(lim.in, zip.out)
    }

    Flow.fromGraph(g).mapMaterializedValue(_._1)
  }

  def forActor(a: ActorRef)(implicit ec: ExecutionContext): RateLimiter = new RateLimiter {
    override def limit[I: ClassTag, O, M](proc: Flow[I, O, M]) = GlobalRateLimiting.limit(a, proc)
  }
}
