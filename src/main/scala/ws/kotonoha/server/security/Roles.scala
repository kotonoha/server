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

package ws.kotonoha.server.security

/**
 * @author eiennohito
 * @since 18.08.12
 */

object Roles extends Enumeration {
  def safeRole(s: String): Option[Role] = {
    values.find(_.toString == s)
  }

  type Role = Value

  /**
   * users with this role can use tools for natural language processing
   * and parsing
   */
  val parsing = Value("parsing")

  /**
   * administrators (can edit roles for everyone and edit global server settings)
   */
  val admin = Value("admin")
}
