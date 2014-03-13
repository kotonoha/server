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

package ws.kotonoha.server.actors.lift

import net.liftweb.actor.LiftActor
import net.liftweb.http.{RenderOut, CometActor}
import net.liftweb.util.Helpers
import net.liftweb.http.js.JsCmds
import net.liftweb.common.{Empty, Full}
import net.liftweb.json.JsonAST.JValue
import net.liftweb.http.js.JE.Call

/**
 * @author eiennohito
 * @since 23.10.12 
 */
trait NgLiftActor extends LiftActor {
  self: CometActor =>

  def module = "kotonoha"

  def svcName: String

  def function = {
    val sb = new StringBuilder()
    sb.append("function($q) {")
    sb.append(
      s"""
        var svc = {};
        svc.fromActor = function(obj) {
          if (svc.callback !== null) {
            svc.callback(obj);
          }
        };
        svc.toActor = function(obj) {
          ${jsonSend.funcId}(obj);
        }
        svc.callback = null;
      """
    )
    global_fnc(sb)
    sb.append("return svc;\n")
    sb.append("}")
    sb.toString()
  }

  def global_fnc(sb: StringBuilder) = {
    sb.append("window.").append(outname).append(" = function(env) {")
    sb.append("svc.fromActor(env);")
    sb.append("};\n")
  }

  lazy val outname = "cometng_" + Helpers.nextFuncName

  def render: RenderOut = {
    val js = s"angular.module('$module').factory('$svcName', $function);"
    RenderOut(Empty, Empty, Full(JsCmds.Run(js) & jsonToIncludeInCode), Empty, ignoreHtmlOnJs = true)
  }

  def ngMessage(msg: JValue) = {
    val jsm = Call(outname, msg).cmd
    self.partialUpdate(jsm)
  }
}
