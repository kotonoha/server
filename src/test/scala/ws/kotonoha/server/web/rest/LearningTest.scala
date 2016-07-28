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

import net.liftweb.mockweb.MockWeb
import net.liftweb.util.LiftFlowOfControlException
import org.scalatest.{FunSuite, Matchers}


/**
 * @author eiennohito
 * @since 09.02.12
 */


class LearningTest extends FunSuite with Matchers {
  
  val learn = new LearningRest {
    val akkaServ = null
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
