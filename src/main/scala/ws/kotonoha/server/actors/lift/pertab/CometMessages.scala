/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

package ws.kotonoha.server.actors.lift.pertab

import net.liftweb.common.Box
import net.liftweb.http.BaseCometActor

/**
 * These are the message we pass around to
 * register each named comet actor with a dispatcher that
 * only updates the specific version it monitors
 */
case class RegisterCometActor(actor: BaseCometActor, name: Box[String])
case class UnregisterCometActor(actor: BaseCometActor)
case class CometName(name: String)
