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

import akka.NotUsed
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.typesafe.scalalogging.StrictLogging
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONDocument

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * @author eiennohito
  * @since 2016/08/23
  */
class MongoSourceStage(cur: Cursor[BSONDocument], limit: Int) extends GraphStage[SourceShape[BSONDocument]] with StrictLogging {
  val out = Outlet[BSONDocument]("MongoSource.out")
  val shape = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) with OutHandler {
    setHandler(out, this)

    private val docs = new mutable.Queue[BSONDocument]()
    @volatile private var getNext: Promise[Cursor.State[NotUsed]] = _
    private var inRequest = true
    private var finished = false

    override def preStart(): Unit = {
      val callback = getAsyncCallback[(Iterator[BSONDocument], Promise[Cursor.State[NotUsed]])] {
        case (r: Iterator[BSONDocument], p: Promise[Cursor.State[NotUsed]]) =>
          val curlen = docs.length
          docs ++= r
          val diff = docs.length - curlen
          logger.trace(s"pack of docs from mongo: $curlen -> ${curlen + diff}")
          inRequest = false
          getNext = p
          if (isAvailable(out)) {
            onPull()
          }
      }

      val finishCallback = getAsyncCallback { (c: Try[NotUsed]) =>
        c match {
          case Success(_) =>
            finished = true
            if (isAvailable(out)) { onPull() }
          case Failure(e) => failStage(e)
        }
      }

      implicit val ec: ExecutionContext = materializer.executionContext

      cur.foldBulksM[NotUsed](NotUsed, limit) { (_, docs) =>
        val p = Promise[Cursor.State[NotUsed]]
        callback.invoke((docs, p))
        p.future
      }.onComplete(finishCallback.invoke)
    }

    override def onDownstreamFinish(): Unit = {
      finished = true
      logger.trace(s"downstream finished, still have ${docs.length} objs")
      if (!inRequest) {
        getNext.success(Cursor.Done(NotUsed))
      }
    }

    private def nextRequest() = {
      inRequest = true
      getNext.success(Cursor.Cont(NotUsed))
    }

    override def onPull(): Unit = {
      if (docs.nonEmpty) {
        push(out, docs.dequeue())
      } else if (finished) {
        completeStage()
      }

      if (!inRequest && docs.size < 30) {
        nextRequest()
      }
    }
  }
}
