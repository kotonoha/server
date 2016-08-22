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

package ws.kotonoha.server.util

import akka.actor.ActorRef
import akka.stream.ThrottleMode.Shaping
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestActorRef
import ws.kotonoha.akane.akka.{MaxAtOnceActor, RateLimitCfg, RateLimitTracing}
import ws.kotonoha.server.test.AkkaFree

import scala.concurrent.{Await, ExecutionContext}

/**
  * @author eiennohito
  * @since 2016/08/22
  */
class GlobalRateLimitingSpec extends AkkaFree {
  implicit def amat = ActorMaterializer.create(kta.system)
  implicit def ec = kta.ioc.inst[ExecutionContext]

  import scala.concurrent.duration._
  "GlobalRateLimiting" - {
    "works with one stream" - {
      var calls = 0

      val cfg = RateLimitCfg(1, 5.seconds, new RateLimitTracing {
        override def finish(ref: ActorRef, tag: Any, start: Long) = calls += 1
        override def start(ref: ActorRef, tag: Any, time: Long) = {}
        override def timeout(ref: ActorRef, tag: Any, start: Long) = {}
      })
      val limiter = TestActorRef[MaxAtOnceActor](MaxAtOnceActor.props(cfg))(kta.system)
      val iseq = (0 until 10).toVector
      val inp = Source(iseq)
      val processor = Flow[Int].map(x => x * 10)
      val limit = GlobalRateLimiting.limit(limiter, processor)
      val result = Await.result(limit.runWith(inp, Sink.fold(0)(_ + _))._2, 1000.second)
      calls shouldBe 10
      result shouldBe 450
    }

    "works with two streams" - {
      var calls = 0

      val cfg = RateLimitCfg(1, 1.minute, new RateLimitTracing {
        override def finish(ref: ActorRef, tag: Any, start: Long) = calls += 1
        override def start(ref: ActorRef, tag: Any, time: Long) = {}
        override def timeout(ref: ActorRef, tag: Any, start: Long) = {}
      })
      val limiter = TestActorRef[MaxAtOnceActor](MaxAtOnceActor.props(cfg))(kta.system)
      val iseq = (0 until 10).toVector
      val inp = Source(iseq)
      val processor = Flow[Int].map(x => x * 10).throttle(1, 1.milli, 1, Shaping)
      val limit = GlobalRateLimiting.limit(limiter, processor)
      val f1 = limit.runWith(inp, Sink.fold(0)(_ + _))._2
      val f2 = limit.runWith(inp, Sink.fold(5)(_ + _))._2
      val result1 = Await.result(f1, 1.second)
      val result2 = Await.result(f2, 1.second)
      calls shouldBe 20
      result1 shouldBe 450
      result2 shouldBe 455
    }
  }
}
