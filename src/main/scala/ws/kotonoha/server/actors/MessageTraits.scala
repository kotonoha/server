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

package ws.kotonoha.server.actors

/**
 * @author eiennohito
 * @since 07.01.13 
 */

trait KotonohaMessage
trait DbMessage extends KotonohaMessage
trait LifetimeMessage extends KotonohaMessage
trait ClientMessage extends KotonohaMessage
trait TokenMessage extends KotonohaMessage
trait DictionaryMessage extends KotonohaMessage
trait SelectWordsMessage extends KotonohaMessage

import language.existentials
case class CreateActor(clz: Class[_], name: String) extends KotonohaMessage
