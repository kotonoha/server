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

package org.eiennohito.kotonoha.web.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers
import net.liftweb.http.SHtml
import org.eiennohito.kotonoha.records.ClientRecord
import org.eiennohito.kotonoha.actors.ioc.{ReleaseAkka, Akka}

/**
 * @author eiennohito
 * @since 25.03.12
 */

trait Clients extends Akka {
  def form(in: NodeSeq): NodeSeq = {
    import Helpers._
    val obj = ClientRecord.createRecord

    def onSave() = {
      obj.save
    }

    bind("cf", in,
      "title" -> obj.name.toForm,
      "submit" -> SHtml.submit ("Save", onSave))
  }
}

object Clients extends Clients with ReleaseAkka
