package org.eiennohito.kotonoha.web.rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.mockweb.MockWeb
import org.scribe.builder.ServiceBuilder
import org.scribe.model.{Token, Verb, OAuthRequest}
import net.liftweb.mocks.MockHttpServletRequest
import net.liftweb.oauth._
import org.scribe.services.TimestampServiceImpl
import net.liftweb.http._
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.oauth.OAuthUtil.Parameter
import org.eiennohito.kotonoha.records.{NonceRecord, UserTokenRecord, ClientRecord}
import org.scalatest.BeforeAndAfterAll
import org.eiennohito.kotonoha.model.{MongoDb, KotonohaApi}
import net.liftweb.util.ThreadGlobal
import org.apache.commons.httpclient.util.EncodingUtil

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



object OAuthBasedService extends OauthRestHelper {
  serve {
    case List("api", "my", "service") Get req => {
      Full(PlainTextResponse("Ok"))
    }
    case List("api", "my", "post") Post req => {
      val v = req.body
      Full(InMemoryResponse(v.get, Nil, Nil, 200))
    }
  }
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


class AuthTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with BeforeAndAfterAll with MongoDb {
  import org.eiennohito.kotonoha.util.SecurityUtil._

  test("oauth get service works") {
    val client = createClient()
    val req = new OAuthRequest(Verb.GET, "http://weabpp.net:8085/k/api/my/service")
    val tok = new Token(token.tokenPublic.is, token.tokenSecret.is)
    client.signRequest(tok, req)

    val mock = OAuthRequestMock(req, "/k")
    val resp = MockWeb.testReq(mock) (OAuthBasedService(_))
    val r = resp()
    r.isEmpty should be (false)
    r match {
      case Full(PlainTextResponse(text, _, code)) => {
        text should equal ("Ok")
        code should equal (200)
      }
      case x @ _ => fail("Got responce we didn't waited for:\n" + x)
    }
  }

  test("oauth post with body service works") {
    val client = createClient()
    val req = new OAuthRequest(Verb.POST, "http://weabpp.net:8085/k/api/my/post")
    req.addPayload("Hello, world!")
    val tok = new Token(token.tokenPublic.is, token.tokenSecret.is)
    client.signRequest(tok, req)

    val mock = OAuthRequestMock(req, "/k")
    val resp = MockWeb.testReq(mock) (OAuthBasedService(_))
    val r = resp()
    r.isEmpty should be (false)
    r match {
      case Full(InMemoryResponse(bytes, _, _, code)) => {
        val text = EncodingUtil.getString(bytes, "UTF-8")
        text should equal ("Hello, world!")
        code should equal (200)
      }
      case x @ _ => fail("Got responce we didn't waited for:\n" + x)
    }
  }

  def createClient() = {
    val api = new KotonohaApi("http://localhost:8080/k/") {
      override def getTimestampService = new TimestampServiceImpl {
      }
    }
    val bldr = new ServiceBuilder().
      provider(api).apiKey(client.apiPublic.is).apiSecret(client.apiPrivate.is);
    bldr.build();
  }

  val client = {
    val clnt = ClientRecord.createRecord
    clnt.name("Test client").apiPrivate(randomHex()).apiPublic(randomHex())
  }

  val token = {
    val tok = UserTokenRecord.createRecord
    tok.label("Test token").user(50L).tokenPublic(randomHex()).tokenSecret(randomHex())
  }

  override protected def beforeAll() {
    client.save
    token.save
  }

  override protected def afterAll() {
    client.delete_!
    token.delete_!
  }
}
