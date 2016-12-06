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

package ws.kotonoha.server.web.lift

import java.lang.invoke.{MethodHandle, MethodHandles, MethodType}
import java.util.concurrent.TimeUnit

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.Provider
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http._
import net.liftweb.util._
import ws.kotonoha.server.ioc.{IocActors, IocSupport}
import ws.kotonoha.server.web.lift.Binders.NodeSeqFn

import scala.concurrent.ExecutionContextExecutor
import scala.xml.NodeSeq

/**
  * @author eiennohito
  * @since 2016/08/09
  */
class SnippetResolverConfig {
  def shortcut(name: String, full: String): SnippetResolverConfig = {
    this.full += name -> full
    this
  }

  def shortcut(name: String, clz: Class[_]): SnippetResolverConfig = {
    this.classes += name -> clz
    this
  }

  def shortcut(name: String, inst: AnyRef): SnippetResolverConfig = {
    this.insts += name -> new InstanceProvider[AnyRef](inst)
    this
  }

  private var full = Map.empty[String, String]
  private var classes = Map.empty[String, Class[_]]
  private var insts = Map.empty[String, Provider[AnyRef]]

  def build(): ShortcutResolver = {
    new ShortcutResolver(full, classes, insts)
  }
}

class ShortcutResolver(full: Map[String, String], classes: Map[String, Class[_]], insts: Map[String, Provider[AnyRef]]) {
  private val shortcut = Full(null)
  def instance(name: String) = insts.get(name)
  def resolveClass(name: String): Box[Class[_]] = {
    if (insts.contains(name)) return shortcut
    val cached = classes.get(name)
    if (cached.isDefined) {
      Full(cached.get)
    } else {
      val item = full.get(name)
      if (item.isEmpty) SnippetResolver.findClass(name)
      else SnippetResolver.findClass(item.get)
    }
  }
}

class SnippetResolver(ioc: IocActors, cfg: SnippetResolverConfig) extends LiftRules.SnippetPF with StrictLogging {
  private val sres = cfg.build()

  private val cache = {
    val bldr = Caffeine.newBuilder()
    bldr.executor(ioc.inst[ExecutionContextExecutor])
    if (Props.devMode) {
      bldr.expireAfterAccess(2, TimeUnit.SECONDS)
    } else {
      bldr.maximumSize(10000L)
    }
    bldr.build[List[String], Box[NodeSeqFn]]()
  }

  private val statefulSnippet = classOf[StatefulSnippet]

  private def findCompanion(clz: Class[_]): Provider[AnyRef] = {
    try {
      val name = clz.getName
      val forSearch = Class.forName(s"$name$$")
      val module = forSearch.getField("MODULE$")
      if (module == null) return null
      val obj = module.get(null)
      InstanceProvider(obj)
    } catch {
      case _: ClassNotFoundException => null
    }
  }

  private val transient = classOf[TransientSnippet]

  private def makeProvider(clz: Class[_]): Provider[AnyRef] = {
    if (IocSupport.checkIfSuitable(clz)) {
      val prov = ioc.provider(Manifest.classType(clz)).asInstanceOf[Provider[AnyRef]]
      if (transient.isAssignableFrom(clz)) {
        prov
      } else new ReqScopedProvider(prov, clz.getSimpleName)
    } else null
  }

  private val nodeSeq: Class[_] = classOf[NodeSeq]
  private val nodeSeqFn: Class[_] = classOf[NodeSeqFn]
  private val simpleMethod = MethodType.methodType(nodeSeq, nodeSeq)

  private val lookup = MethodHandles.publicLookup()

  private def reflectAccess(clz: Class[_ <: AnyRef], instance: Provider[AnyRef], method: String): Box[NodeSeqFn] = {
    var nsf: Box[NodeSeqFn] = Empty

    try {
      val meth = clz.getMethod(method, nodeSeq)
      if (nodeSeq.isAssignableFrom(meth.getReturnType)) {
        val mh = lookup.findVirtual(clz, method, simpleMethod)
        nsf = Full(new ReflectedMethodDispatchInstance(instance, mh))
      }
    } catch {
      case _: NoSuchMethodException =>
    }

    if (nsf.isEmpty) {
      try {
        val meth = clz.getMethod(method)
        if (nodeSeqFn.isAssignableFrom(meth.getReturnType)) {
          val mh = lookup.unreflect(meth)
          nsf = Full(new ReflectedAccessorDispatchInstance(instance, mh))
        }
      } catch {
        case _: NoSuchMethodException =>
      }
    }

    if (nsf.isEmpty) {
      logger.trace(s"for $clz#$method accessor was not found")
    }


    nsf
  }

  private def makeProvider0(prefix: String, clz: Class[_]): Provider[AnyRef] = {
    if (clz == null) return sres.instance(prefix).orNull

    //don't handle stateful snippets
    if (statefulSnippet.isAssignableFrom(clz)) return null

    var instance: Provider[AnyRef] = null

    // what instances can be:
    // 1) object
    // 2) class (injectable)
    // 3) class (non-injectable) //ignore it
    instance = makeProvider(clz)

    if (instance == null) {
      instance = findCompanion(clz)
    }

    instance
  }

  private def makeSnippet0(pfx: String, clz: Class[_], method: String): Box[NodeSeqFn] = {

    val instance = makeProvider0(pfx, clz)

    if (instance == null) return Empty

    instance.get() match {
      case s: DispatchSnippet =>
        if (s.dispatch.isDefinedAt(method)) {
          if (s.getClass.getName.endsWith("$")) { //singleton, so cache dispatch as well
            Full(s.dispatch.apply(method))
          } else {
            Full(new ProvidedDispatchedInstance(instance.asInstanceOf[Provider[DispatchSnippet]], method))
          }
        } else Empty
      case x => reflectAccess(x.getClass, instance, method)
    }
  }

  private def makeSnippet(args: List[String], clz: Class[_], prefix: String,  method: String): Box[NodeSeqFn] = {
    try {
      makeSnippet0(prefix, clz, method)
    } catch {
      case e: Exception =>
        throw SnippetInstantiationException(
          s"could not instantiatiate snippet for args: $args, clz=$clz, method=$method",
          e
        )
    }
  }

  def resolve(x: List[String]): Boolean = {

    val (prefix, method) = x match {
      case List(one) => (one, "render")
      case List(one, two) => (one, two)
      case _ =>
        cache.put(x, Empty)
        return false
    }

    val clzOpt = sres.resolveClass(prefix)

    clzOpt match {
      case Full(c) =>
        val snip = makeSnippet(x, c, prefix, method)
        logger.trace(s"resolved ${x.mkString("(", ",", ")")} to $snip")
        cache.put(x, snip)
        snip.isDefined
      case _ =>
        logger.trace(s"could not find class for ${x.mkString("(", ",", ")")}")
        cache.put(x, Empty)
        false
    }
  }

  override def isDefinedAt(x: List[String]): Boolean = {
    val item = cache.getIfPresent(x)
    if (item == null) {
      return resolve(x)
    }

    item.isDefined
  }

  override def apply(v1: List[String]) = {
    val present = cache.getIfPresent(v1)
    if (present == null) {
      throw new Exception(s"$v1 was null!")
    } else present.openOrThrowException("should be present")
  }
}

object SnippetResolver {
  private val packages = makePackages

  private def makePackages: List[String] = LiftRules.buildPackage("snippet")

  private[lift] def findClass(prefix: String): Box[Class[AnyRef]] = {
    val ps = if (Props.devMode) {
      makePackages
    } else packages
    ClassHelpers.findClass(prefix, ps)
  }
}

case class InstanceProvider[T](get: T) extends Provider[T]

class ReqScopedProvider[T](inner: Provider[T], name: String) extends RequestVar[T](inner.get()) with Provider[T] {
  override protected val __nameSalt = s"template_$name"
}


trait SnippetAccess extends NodeSeqFn

final class ProvidedDispatchedInstance(provider: Provider[DispatchSnippet], method: String) extends SnippetAccess {
  override def apply(v1: NodeSeq) = {
    provider.get().dispatch.apply(method).apply(v1)
  }

  override def toString() = s"DS: $method"
}

final class ReflectedMethodDispatchInstance(provider: Provider[AnyRef], method: MethodHandle) extends SnippetAccess {
  override def apply(v1: NodeSeq) = {
    val inst = provider.get()
    method.invoke(inst, v1): NodeSeq
  }

  override def toString() = s"RM: $method"
}

final class ReflectedAccessorDispatchInstance(provider: Provider[AnyRef], method: MethodHandle) extends SnippetAccess {
  override def apply(v1: NodeSeq) = {
    val inst = provider.get()
    val meth = method.invoke(inst): NodeSeqFn
    meth.apply(v1)
  }

  override def toString() = s"RA: $method"
}

case class SnippetInstantiationException(msg: String, internal: Exception) extends RuntimeException(msg, internal)
