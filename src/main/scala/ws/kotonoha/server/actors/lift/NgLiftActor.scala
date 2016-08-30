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
import net.liftweb.http.{CometActor, RenderOut}
import net.liftweb.util.Helpers
import net.liftweb.http.js.JsCmds
import net.liftweb.common.{Empty, Full}
import net.liftweb.json.JsonAST.JValue
import net.liftweb.http.js.JE.Call
import ws.kotonoha.lift.json.JWrite

/**
 * @author eiennohito
 * @since 23.10.12 
 */
trait NgLiftActor extends LiftActor {
  self: CometActor =>

  protected def module = "kotonoha"

  protected def svcName: String

  private def function = {
    val sb = new StringBuilder()
    sb.append("function($q) {")
    sb.append(
      s"""
        var svc = {};
        var callbacks = [];
        svc.fromActor = function(obj) {
          if (svc.callback !== null) {
            svc.callback(obj);
          }
          for (var i = 0; i < callbacks.length; ++i) { callbacks[i](obj); }
        };
        svc.toActor = function(obj) {
          ${jsonSend.funcId}(obj);
        }
        svc.callback = null;
        svc.onMessage = function(f) {
          var rcved = window.${linkUid}_arr || [];
          callbacks.push(f);
          for (var i = 0; i < rcved.length; ++i) { svc.fromActor(rcved[i]); }
          window.${linkUid}_arr = [];
        }
      """
    )
    global_fnc(sb)
    sb.append("return svc;\n")
    sb.append("}")
    sb.toString()
  }

  private def global_fnc(sb: StringBuilder) = {
    sb.append("window.").append(outname).append(" = function(env) {")
    sb.append("svc.fromActor(env);")
    sb.append("};\n")
  }

  private val linkUid = Helpers.nextFuncName
  private val outname = "cometng_" + linkUid

  private def initialFunction =
    s"""
     if (!window.$outname) {
       window.${linkUid}_arr = [];
       window.$outname = function(o) {window.${linkUid}_arr.push(o);}
     }
    """

  def render: RenderOut = {
    val factoryCode = s"angular.module('$module').factory('$svcName',['$$q', $function]);"
    val cmds = JsCmds.Run(initialFunction) & JsCmds.Run(factoryCode) & jsonToIncludeInCode
    RenderOut(Empty, Full(defaultHtml), Full(cmds), Empty, ignoreHtmlOnJs = true)
  }

  protected def ngMessageRaw(msg: JValue): Unit = {
    val jsm = Call(outname, msg).cmd
    self.partialUpdate(jsm)
  }

  protected def ngMessage[T](msg: T)(implicit jw: JWrite[T]): Unit = {
    ngMessageRaw(jw.write(msg))
  }
}
