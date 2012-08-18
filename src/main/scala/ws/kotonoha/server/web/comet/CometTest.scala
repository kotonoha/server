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

package ws.kotonoha.server.web.comet

import net.liftweb.util.Schedule
import net.liftweb.http.js.JsCmds.SetHtml
import org.joda.time.DateTime
import xml.Text
import net.liftweb.http.CometActor

class CometTest extends CometActor {

  import net.liftweb.util.Helpers._

  var rnd: Double = _

  def render = {
    Schedule.schedule(this, TimeMessage, 1 second)
    rnd = math.random
    <span id="time">Current time here</span>
  }


  override def lowPriority = {
    case TimeMessage => {
      partialUpdate(SetHtml("time", Text(rnd + new DateTime().toString)))
      Schedule.schedule(this, TimeMessage, 2 seconds)
    }
  }

}

case object TimeMessage
