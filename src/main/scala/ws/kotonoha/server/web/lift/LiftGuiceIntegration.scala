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

import java.util.function.Supplier

import com.google.inject.Injector
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.http.{CometActorSetupHelper, CometCreationInfo, LiftCometActor, LiftRules}
import net.liftweb.util.{Helpers, LoanWrapper, Vendor}
import org.bson.types.ObjectId
import ws.kotonoha.server.ioc._
import ws.kotonoha.server.records.UserRecord

/**
  * @author eiennohito
  * @since 2016/08/19
  */
class LiftGuiceIntegration(inj: Injector) extends StrictLogging {

  import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector

  private val iocGlobal = ThreadLocal.withInitial(new Supplier[IocActors] {
    override def get() = {
      val childInjector = inj.createChildInjector(new AnonUserModule)
      val obj = childInjector.getInstance(classOf[AnonUserActorsIoc])
      obj
    }
  })

  def wrapUser() = new LoanWrapper {
    override def apply[T](f: => T) = {
      UserRecord.currentId match {
        case Full(uid) => withUser(uid)(f)
        case _ => f
      }
    }
  }

  private[this] val ucx = inj.instance[UserContextService]

  @inline final def withUser[T](uid: ObjectId)(f: => T): T = {
    val toRestore = iocGlobal.get()
    try {
      iocGlobal.set(ucx.of(uid))
      logger.trace(s"using context for user $uid")
      f
    } finally {
      iocGlobal.set(toRestore)
    }
  }

  def cometCreation(): Vendor[(CometCreationInfo) => Box[LiftCometActor]] = {
    Vendor(internalCreateComet _)
  }

  private[this] val cometClz = classOf[LiftCometActor]

  private def internalCreateComet(cci: CometCreationInfo): Box[LiftCometActor] = {
    val tpe = cci.cometType
    val clz = Helpers.findClass(tpe, LiftRules.buildPackage("comet"))
    clz.flatMap { c =>
      if (IocSupport.checkIfSuitable(c) && cometClz.isAssignableFrom(c)) {
        val actor = iocActors.inst(Manifest.classType(c)).asInstanceOf[LiftCometActor]
        CometActorSetupHelper.setup(actor, Full(tpe), cci)
        Full(actor)
      } else Empty
    }
  }

  val iocActors = new IocActors {
    override def props[T: Manifest] = iocGlobal.get().props[T]

    override def provider[T: Manifest] = iocGlobal.get().provider[T]
  }

  def snippetResolver(src: SnippetResolverConfig) = new SnippetResolver(iocActors, src)
}
