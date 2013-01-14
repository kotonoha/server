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

package ws.kotonoha.server.akka

import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import scala.concurrent.Await


case object A2

class Actor1 extends Actor {
  lazy val act2 = context.actorOf(Props[Actor2], "a2")
  override def receive = {
    case A2 => {
      sender ! act2
    }
  }
}

class Actor2 extends Actor {
  override def receive = {
    case _ =>
  }
}

class AkkaTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  import akka.pattern.ask
  import concurrent.duration._
  test("Paths in actor systems resolve as is should") {
    val sys = ActorSystem("test")
    val a1 = sys.actorOf(Props[Actor1], "a1")
    val fut = ask(a1, A2)(10 seconds).mapTo[ActorRef]

    val a2 = Await.result(fut, 100 second)
    a2 should not be (null)
  }
}
