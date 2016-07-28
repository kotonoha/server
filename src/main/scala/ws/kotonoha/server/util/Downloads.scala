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

package ws.kotonoha.server.util

import java.net.{Proxy, URI}
import java.nio.file.{Path, StandardOpenOption}

import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.io.IOUtils


/**
  * @author eiennohito
  * @since 2016/07/27
  */
object Downloads extends StrictLogging {
  import ws.kotonoha.akane.resources.FSPaths._

  def download(uri: URI, out: Path, proxy: Proxy = null) = {
    logger.debug(s"downloading $uri to $out")
    val url = uri.toURL
    val conn = if (proxy == null) url.openConnection() else url.openConnection(proxy)

    for {
      is <- conn.getInputStream.res
      os <- out.outputStream(StandardOpenOption.CREATE)
    } {
      val bytes = IOUtils.copy(is, os)
      logger.debug(s"download of $uri is successful: got $bytes")
    }

  }
}
