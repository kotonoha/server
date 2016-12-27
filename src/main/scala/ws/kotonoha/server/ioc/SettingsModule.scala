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

import com.google.inject.Provides
import net.codingwell.scalaguice.ScalaModule
import ws.kotonoha.server.records.{AppConfig, KotoSettings, UserSettings}

/**
  * @author eiennohito
  * @since 2016/12/22
  */
class SettingsModule extends ScalaModule {
  override def configure(): Unit = {}

  @Provides
  def kotoConfig(): KotoSettings = AppConfig.apply()

  @Provides
  def userConfig(uc: UserContext): UserSettings = uc.settings
}
