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

package ws.kotonoha.server

import java.nio.file.{Files, Paths}

import net.liftweb.util.Props
import ws.kotonoha.akane.juman.JumanPipeExecutor
import com.typesafe.config.{ConfigException, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import ws.kotonoha.akane.config.Configuration
import ws.kotonoha.akane.pipe.knp.KnpTreePipeParser

import scala.concurrent.ExecutionContext

/**
 * @author eiennohito
 * @since 20.08.12
 */

object KotonohaConfig extends StrictLogging {

  lazy val config = {
    import scala.collection.JavaConversions._
    val props = Props.props
    val base = ConfigFactory.defaultApplication(KotonohaConfig.getClass.getClassLoader).withFallback(ConfigFactory.defaultOverrides())
    val fromLift = ConfigFactory.parseMap(props)
    var initial = Configuration.rawConfigFor("kotonoha")
    val trial = initial.withFallback(base).resolve()
    if (trial.hasPath("cfg.include")) {
      val includes = trial.getStringList("cfg.include")
      val iter = includes.iterator()
      while (iter.hasNext) {
        val path = Paths.get(iter.next())
        logger.debug("including a config file from {}", path)
        if (Files.exists(path)) {
          val parsed = ConfigFactory.parseFileAnySyntax(path.toFile)
          initial = parsed.withFallback(initial)
        }
      }
    }
    initial.withFallback(fromLift).withFallback(base).resolve()
  }

  def jumanExecutor = {
    JumanPipeExecutor.apply(config)
  }

  def knpExecutor(implicit ec: ExecutionContext) = {
    KnpTreePipeParser.apply(config)
  }

  def safe[T](f: => T): Option[T] = try {
    Some(f)
  } catch {
    case e: ConfigException.Missing => None
    case e: ConfigException.WrongType => None
  }

  def string(name: String) = config.getString(name)
  def safeString(name: String) = safe { string(name) }

  def int(name: String) = config.getInt(name)
  def safeInt(name: String) = safe { int(name) }
}
