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

package ws.kotonoha.server.ops

import akka.NotUsed
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import reactivemongo.api.commands.{MultiBulkWriteResult, UpdateWriteResult, WriteResult}
import ws.kotonoha.model.WordStatus
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.records.WordRecord

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object OpsExtensions {

  implicit def rmfuture2extended[T: WtfWriteResult](f: Future[T]): RMFutureExt[T] = new RMFutureExt[T](f)

  class RMFutureExt[T](val f: Future[T]) extends AnyVal {
    @inline def minMod[R](n: Int, fn: => R = NotUsed)(implicit ec: ExecutionContext, w: WtfWriteResult[T]): Future[R] = {
      f.map(x => {
        mongoOk(w.ok(x))
        passWithMore(w.n(x), n)
        fn
      })
    }

    @inline def maxMod[R](n: Int, fn: => R = NotUsed)(implicit ec: ExecutionContext, w: WtfWriteResult[T]): Future[R] = {
      f.map(x => {
        mongoOk(w.ok(x))
        passWithLess(w.n(x), n)
        fn
      })
    }

    @inline def mod[R](n: Int, fn: => R = NotUsed)(implicit ec: ExecutionContext, w: WtfWriteResult[T]): Future[R] = {
      f.map { x =>
        mongoOk(w.ok(x))
        passWithExact(w.n(x), n)
        fn
      }
    }

    @inline def isOk[R](fn: => R = NotUsed)(implicit ec: ExecutionContext, w: WtfWriteResult[T]): Future[R] = {
      f.map(x => {
        mongoOk(w.ok(x))
        fn
      })
    }
  }

  def passWithExact(x: Int, n: Int): Unit = {
    if (x != n) {
      error(s"required number of modifications was $x instead of $n")
    }
  }

  def passWithLess(x: Int, n: Int): Unit = {
    if (x > n) {
      error(s"number of items $x was greater than expected $n")
    }
  }

  def passWithMore(x: Int, n: Int): Unit = {
    if (x < n) {
      error(s"number of items $x was lesser than expected $n")
    }
  }

  def mongoOk(x: Boolean): Unit = {
    if (!x) {
      error("error in mongo")
    }
  }

  def error(s: String): Nothing = {
    throw new OpsException(s)
  }
}

trait WtfWriteResult[T] {
  def ok(o: T): Boolean
  def n(o: T): Int
}

object WtfWriteResult {
  implicit object multibulk extends WtfWriteResult[MultiBulkWriteResult] {
    override def ok(o: MultiBulkWriteResult) = o.ok
    override def n(o: MultiBulkWriteResult) = o.n
  }

  implicit object update extends WtfWriteResult[UpdateWriteResult] {
    override def ok(o: UpdateWriteResult) = o.ok
    override def n(o: UpdateWriteResult) = o.nModified
  }

  implicit object generic extends WtfWriteResult[WriteResult] {
    override def ok(o: WriteResult) = o.ok
    override def n(o: WriteResult) = o.n
  }
}

class OpsException(msg: String) extends RuntimeException(msg)
