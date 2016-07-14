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

import com.google.inject.Inject
import net.liftweb.actor.LiftActor
import net.liftweb.json.JsonAST.{JArray, JInt, JString}
import ws.kotonoha.server.actors.ioc.ReleaseAkka
import ws.kotonoha.server.actors.lift.{AkkaInterop, NgLiftActor}

import scala.concurrent.ExecutionContext

/**
 * @author eiennohito
 * @since 22.10.12 
 */

class TestActor @Inject() (ec: ExecutionContext) extends LiftActor with NgLiftActor with AkkaInterop with ReleaseAkka {
  import ws.kotonoha.server.util.KBsonDSL._

  override protected def dontCacheRendering = true

  def svcName = "testSvc"

  override def receiveJson = {
    case JString("asdf") => ngMessage(JInt(10))
    case JString(s) => ngMessage(("hey" -> 5) ~ ("length" -> s.length))
    case JArray(JString(x) :: _) => ngMessage(("hey" -> 5) ~ ("length" -> x.length))
  }
}
