package org.eiennohito.kotonoha.web.rest

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.eiennohito.kotonoha.actors.ioc.Akka
import org.eiennohito.kotonoha.actors.AkkaMain
import akka.testkit.TestActorRef
import akka.actor.{Actor, ActorSystem}
import net.liftweb.http.rest.ContinuationException
import net.liftweb.http.Req
import net.liftweb.mockweb.MockWeb
import net.liftweb.util.LiftFlowOfControlException

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
/**
 * @author eiennohito
 * @since 09.02.12
 */

object MockAkka extends AkkaMain {
  val system = ActorSystem("kototest")

  implicit val s = system

  val wordRegistry = TestActorRef(new Actor {
    protected def receive = {
      case _ =>
    }
  })

  val wordSelector = TestActorRef(new Actor {
    protected def receive = {
      case _ =>
    }
  })

  val markProcessor = TestActorRef(new Actor {
    protected def receive = {
      case _ =>
    }
  })
}

class LearningTest extends FunSuite with ShouldMatchers {
  
  val learn = new LearningRest {
    val akkaServ = MockAkka
  }
  
  test("learning lookups well") {
    val ce = intercept[LiftFlowOfControlException] {
      MockWeb.testReq("api/words/list/30") { req =>
        learn(req)
      }      
    }
  }

}
