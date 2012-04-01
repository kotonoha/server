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
package org.eiennohito.kotonoha.rest;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/**
 * @author eiennohito
 * @since 22.02.12
 */
public class KotonohaApi extends DefaultApi10a {

  private String baseUri;
  private String requestToken;
  private String accessToken;
  private String authorizationUrl;

  public KotonohaApi(String baseUri) {
    this.baseUri = baseUri;
    requestToken = baseUri + "/oauth/requset_token";
    accessToken = baseUri + "/oauth/access_token";
    authorizationUrl = baseUri + "/oauth/authorization";
  }

  @Override
  public String getRequestTokenEndpoint() {
    return requestToken;
  }

  @Override
  public String getAccessTokenEndpoint() {
    return accessToken;
  }

  @Override
  public String getAuthorizationUrl(Token requestToken) {
    return authorizationUrl;
  }

}
