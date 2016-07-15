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

package ws.kotonoha.server.actors.schedulers

import org.bson.types.ObjectId

/**
 * @author eiennohito
 * @since 02.03.13 
 */

class SmallSchedulersTest extends AkkaFree {
  val uid = new ObjectId()

  val uctx = kta.userContext(uid)

  "new scheduler" - {
    "don't go forever" in {

    }
  }
}
