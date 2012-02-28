package org.eiennohito.kotonoha.web.rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.mockweb.MockWeb
import net.liftweb.http.{S, LiftRules, PlainTextResponse, InMemoryResponse}
import org.scribe.builder.ServiceBuilder
import org.eiennohito.kotonoha.model.KotonohaApi
import org.scribe.model.{Token, Verb, OAuthRequest}
import net.liftweb.mocks.MockHttpServletRequest
import net.liftweb.oauth._
import net.liftweb.common.{Empty, Full}
import org.scribe.services.TimestampServiceImpl

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

class SimpleService extends RestHelper {
  serve {
    case "cool" :: "service" :: Nil Get req => {
      Full(PlainTextResponse("Ok"))
    }
  }
}

class DummyConsumer(val consumerKey: String, val consumerSecret: String) extends OAuthConsumer {
  def reset {}

  def enabled = 0

  def user = null

  def title = null

  def applicationUri = null

  def callbackUri = null

  def xdatetime = null
}

object OAuthRequestMock {
  def apply(req: OAuthRequest, cp: String = "/"): MockHttpServletRequest = {
    import scala.collection.JavaConversions._
    val out = new MockHttpServletRequest(req.getUrl, cp)
    out.body_=(req.getBodyContents)
    out.headers ++= (req.getHeaders.map  {case (k, v) => k -> List(v)}.toSeq)
    out.method = req.getVerb.name()
    out
  }
}


class AuthTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  test("plain service works") {
    val s = new SimpleService
    val r = MockWeb.testReq("/cool/service") { req =>
      s(req)
    }
    r().isEmpty should be (false)
  }

  object NonceMeta extends OAuthNonceMeta {
    def create(consumerKey: String, token: String, timestamp: Long, nonce: String) {}

    def find(consumerKey1: String, token1: String, timestamp1: Long, nonce1: String) = {
      Empty
    }

    def bulkDelete_!!(minTimestamp: Long) {}
  }

  test("nothing") {
    val apiKey = "dpf43f3p2l4k3l03"
    val apiSecret = "kd94hf93k423kf44"
    val tokenStr = "nnch734d00sl2jdk"
    val tokenSec = "pfkkdhi9sl3r4s00"
    val api = new KotonohaApi("http://localhost:8080/k/") {
      override def getTimestampService = new TimestampServiceImpl {
      }
    }
    val bldr = new ServiceBuilder().
      provider(api).apiKey(apiKey).apiSecret(apiSecret);
    val serv = bldr.build();
    val req = new OAuthRequest(Verb.GET, "http://localhost:8080/k/api/words/scheduled/10");
    val token = new Token(tokenStr, tokenSec);
    serv.signRequest(token, req);

    val mock = OAuthRequestMock(req, "/k")
    MockWeb.testReq(mock) { r =>
      val oar = new HttpRequestMessage(r)
      val validator = new OAuthValidator {
        protected def oauthNonceMeta = NonceMeta
      }
      val box = validator.validateMessage(oar, new OAuthAccessor(new DummyConsumer(apiKey, apiSecret), Full(tokenSec), Empty))
      val i = 0;
    }



  }
}