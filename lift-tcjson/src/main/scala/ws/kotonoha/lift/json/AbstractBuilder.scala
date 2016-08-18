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

package ws.kotonoha.lift.json

import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.json.JsonAST.JField

/**
  * @author eiennohito
  * @since 2016/08/18
  */
abstract class AbstractBuilder[T] {
  def appendField(fld: JField)
  def result(): Box[T]
  protected var errors: Box[Failure] = Empty
  def addError(f: Failure) = errors = Full(f.copy(chain = errors))
}
