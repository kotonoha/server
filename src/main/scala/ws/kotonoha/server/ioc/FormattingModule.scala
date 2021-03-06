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

package ws.kotonoha.server.ioc

import javax.inject.Inject

import com.google.inject.Provider
import net.codingwell.scalaguice.ScalaModule
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import ws.kotonoha.server.web.lift.Render

import scala.xml.Text

/**
  * @author eiennohito
  * @since 2016/08/08
  */
class FormattingModule extends ScalaModule {
  override def configure() = {
    bind[DateFormatting].toProvider[UserDateFormatterProvider]
  }
}

class UserDateFormatterProvider @Inject() (
) extends Provider[DateFormatting] {
  override def get() = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm")
    new FormatterFormatting(formatter)
  }
}

trait DateFormatting extends Render[DateTime] {
  def format(d: DateTime): String
  override def render(o: DateTime) = Text(format(o))
}

final private[ioc] class FormatterFormatting(fmt: DateTimeFormatter) extends DateFormatting {
  override def format(d: DateTime) = fmt.print(d)
}
