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

package net.liftweb.http

import javax.servlet.AsyncContext
import javax.servlet.http.HttpServletResponse

import com.typesafe.scalalogging.StrictLogging
import net.liftweb.common.Full
import net.liftweb.http.provider.servlet.{AsyncProviderMeta, HTTPRequestServlet, HTTPResponseServlet, ServletAsyncProvider}
import net.liftweb.http.provider.{HTTPRequest, RetryState}

/**
  * @author eiennohito
  * @since 2016/08/10
  */
class Servlet31AsyncProviderMeta extends AsyncProviderMeta {
  override def suspendResumeSupport_? = true
  override def providerFunction = {
    Full(r => new Servlet31AsyncProvider(r))
  }
}

class Servlet31AsyncProvider(req: HTTPRequest) extends ServletAsyncProvider with StrictLogging {
  private[this] val servletReq = req.asInstanceOf[HTTPRequestServlet].req

  override def suspendResumeSupport_? = {
    val supported = servletReq.isAsyncSupported
    logger.trace(s"check if request to $req is supported: $supported")
    supported
  }

  override def resumeInfo = None

  private[this] var ctx: AsyncContext = null

  override def suspend(timeout: Long) = {
    ctx = servletReq.startAsync()
    ctx.setTimeout(timeout)
    logger.trace("Servlet 3.1 suspend")
    RetryState.SUSPENDED
  }

  override def resume(ref: (Req, LiftResponse)) = {
    val resp = ctx.getResponse.asInstanceOf[HttpServletResponse]
    val respWrappet = new HTTPResponseServlet(resp)
    val liftServlet = req.provider.liftServlet
    liftServlet.sendResponse(ref._2, respWrappet, ref._1)
    ctx.complete()
    logger.trace("Servlet 3.1 resume")
    true
  }
}


