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

import xml.NodeSeq
import net.liftweb.util.Helpers
import ws.kotonoha.server.actors.ioc.{ReleaseAkka, Akka}
import ws.kotonoha.server.actors.{CreateQrWithLifetime, CreateQr}

/**
 * @author eiennohito
 * @since 24.03.12
 */

object QrAuth extends Akka with ReleaseAkka {
  import concurrent.duration._
  import akka.pattern.ask

  val duration = 1 second
  def qrcode(in: NodeSeq): NodeSeq = {
    import Helpers._

    //val msg = CreateQrWithLifetime()

    //val obj = ask(akkaServ.root, CreateQr()
    //bind("qr", in, "code")
    in
  }
}
