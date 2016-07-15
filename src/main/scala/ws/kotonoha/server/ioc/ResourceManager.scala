/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/07/14
  */

trait Res extends AutoCloseable {
  def register[T <: AutoCloseable](res: T)(implicit ct: ClassTag[T]): Unit
  def make[T <: AutoCloseable](f : => T)(implicit ct: ClassTag[T]): T
  def makeOrCached[T <: AutoCloseable](key: AnyRef, f: => T)(implicit ct: ClassTag[T]): T
}

class ResourceManager extends Res {
  private[this] val resources = new AtomicReference[List[AutoCloseable]](Nil)
  private[this] val keyCache = new ConcurrentHashMap[AnyRef, AutoCloseable]()

  @tailrec
  private def doRegister(res: AutoCloseable): Boolean = {
    val present = resources.get()
    if (!present.exists(_ eq res)) {
      val upd = res :: present
      if (!resources.compareAndSet(present, upd)) {
        doRegister(res)
      } else true
    } else false
  }

  override def register[T <: AutoCloseable](res: T)(implicit ct: ClassTag[T]): Unit = {
    doRegister(res)
  }

  override def make[T <: AutoCloseable](f: => T)(implicit ct: ClassTag[T]) = {
    makeOrCached(ct.runtimeClass, f)
  }

  override def makeOrCached[T <: AutoCloseable](key: AnyRef, f: => T)(implicit ct: ClassTag[T]) = {
    val init = keyCache.get(key)
    if (init == null) {
      val obj = resources.synchronized { f }
      val present = keyCache.put(key, obj)
      if (present == null) {
        register(obj)
        obj
      } else {
        obj.close()
        present.asInstanceOf[T]
      }
    } else init.asInstanceOf[T]
  }

  override def close() = {
    resources.synchronized {
      resources.get().foreach(_.close()) //reverse construction order is correct
      resources.set(Nil) //free references
      keyCache.clear()
    }
  }
}
