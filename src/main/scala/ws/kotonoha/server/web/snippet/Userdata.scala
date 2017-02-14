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

package ws.kotonoha.server.web.snippet

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import net.liftweb.http.S
import ws.kotonoha.server.ioc.UserContext
import ws.kotonoha.server.records.UserRecord

import scala.xml.NodeSeq

/**
  * @author eiennohito
  * @since 2017/02/13
  */
class Userdata @Inject()(uc: UserContext) extends StrictLogging {

  import ws.kotonoha.server.web.lift.Binders._

  def changePassword(in: NodeSeq): NodeSeq = {
    val oldpass = S.param("old-pass").openOr("")
    val newpass = S.param("new-pass").openOr("")
    val newpass2 = S.param("new-pass2").openOr("")

    val user = UserRecord.currentUser.openOrThrowException("the user should be logged in to be here!")

    val newPassTransform =
      "@new-pass [value]" #> newpass &
      "@new-pass2 [value]" #> newpass2

    val oldpassTransform = "@old-pass [value]" #> oldpass

    val tf = if (oldpass == "" && newpass == "" && newpass2 == "") {
      oldpassTransform
    } else if (!user.password.isMatch(oldpass)) {
      newPassTransform &
      "#pwd-notices *" #> <span class="alert alert-warning">Old password is invalid.</span>
    } else if (!newpass.equals(newpass2)) {
      oldpassTransform &
      "#pwd-notices *" #> <span class="alert alert-warning">New password does not match its confirmation.</span>
    } else if (newpass.length < 6) {
      oldpassTransform &
      "#pwd-notices *" #> <span class="alert alert-warning">New password should be longer than 6 characters</span>
    } else {
      user.password.setPassword(newpass)
      user.save()
      logger.debug(s"user ${uc.uid} has changed password")
      "#pwd-notices *" #> <span class="alert alert-success">New password has been set!</span>
    }

    tf(in)
  }

}
