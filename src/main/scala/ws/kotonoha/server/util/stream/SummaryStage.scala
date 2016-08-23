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

package ws.kotonoha.server.util.stream

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

/**
  * @author eiennohito
  * @since 2016/08/23
  */
class SummaryStage[T] extends GraphStage[FlowShape[T, Seq[T]]] {
  val in = Inlet[T]("Summary.in")
  val out = Outlet[Seq[T]]("Summary.out")
  val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) with InHandler with OutHandler {
    val bldr = Seq.newBuilder[T]
    setHandlers(in, out, this)

    override def preStart(): Unit = {
      pull(in)
    }

    override def onPush() = {
      bldr += grab(in)
      pull(in)
    }

    override def onPull() = {}

    override def onUpstreamFinish(): Unit = {
      if (isAvailable(out)) {
        push(out, bldr.result())
      }
      completeStage()
    }
  }
}
