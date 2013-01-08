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
package ws.kotonoha.server.web.rest

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.TestActorRef
import akka.actor.{Actor, ActorSystem}
import net.liftweb.mockweb.MockWeb
import net.liftweb.util.LiftFlowOfControlException
import ws.kotonoha.server.actors.{GlobalActor, UserGuardActor, AkkaMain}


/**
 * @author eiennohito
 * @since 09.02.12
 */

object MockAkka extends AkkaMain {
  implicit val system = ActorSystem("kototest")

  val global = TestActorRef(new GlobalActor, "global")
}

class LearningTest extends FunSuite with ShouldMatchers {
  
  val learn = new LearningRest {
    val akkaServ = MockAkka
  }

  //our apis don't eat requests without authorization
  ignore("learning lookups well") {
    val ce = intercept[LiftFlowOfControlException] {
      MockWeb.testReq("http://kotonoha.ws/api/words/scheduled/30") { req =>
        learn(req)
      }      
    }
  }

}
