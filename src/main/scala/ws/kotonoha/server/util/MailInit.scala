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

import javax.mail.{Authenticator, PasswordAuthentication}

import com.typesafe.config.Config
import net.liftweb.common.Full
import net.liftweb.util.Mailer

/**
  * @author eiennohito
  * @since 2016/08/05
  */
object MailInit {
  import ws.kotonoha.akane.config.ScalaConfig._

  private val keys = Seq(
    "mail.smtp.host",
    "mail.smtp.port"
  )

  private def extactOpts(cfg: Config): Map[String, String] = {
    val bldr = Map.newBuilder[String, String]
    for (k <- keys) {
      bldr += k -> cfg.getString(k)
    }
    bldr.result()
  }

  def init(cfg: Config) = {

    val enabled = cfg.optBool("mail.enabled").getOrElse(false)
    if (enabled) {
      Mailer.customProperties = Map (
        "mail.smtp.starttls.enable" -> "true",
        "mail.smtp.ssl.enable" -> "true",
        "mail.smtp.auth" -> "true"
      ) ++ extactOpts(cfg)

      Mailer.authenticator = Full(new Authenticator {
        override def getPasswordAuthentication = {
          new PasswordAuthentication(
            cfg.getString("mail.smtp.username"),
            cfg.getString("mail.smtp.password")
          )
        }
      })
    }
  }
}
