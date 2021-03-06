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

package ws.kotonoha.server.web.snippet

import xml.{UnprefixedAttribute, MetaData}

/**
 * Classpath resources
 */
class Cp {
  import ClasspathResource._
  def render(in: MetaData): MetaData = {
    new UnprefixedAttribute("id", "help", scala.xml.Null)
  }

  def mhref(in: MetaData): MetaData = {
    attr("href", js(min(in.value.text)))
  }


  def attr(link: String, value: String): UnprefixedAttribute = {
    new UnprefixedAttribute(link, value, scala.xml.Null)
  }

  def href(in: MetaData): MetaData = {
    in
  }
}
