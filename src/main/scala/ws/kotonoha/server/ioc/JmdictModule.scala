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

import java.nio.file.Paths

import com.google.inject.{Provides, Singleton}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.store.MMapDirectory
import ws.kotonoha.dict.jmdict.{LuceneJmdict, LuceneJmdictImpl}

import scala.concurrent.ExecutionContextExecutor

/**
  * @author eiennohito
  * @since 2016/07/21
  */
class JmdictModule extends ScalaModule {
  override def configure() = {}

  @Provides
  @Singleton
  def luceneJmdict(
    ec: ExecutionContextExecutor,
    res: Res,
    cfg: Config
  ): LuceneJmdict = {
    val jdictPath = cfg.getString("jmdict.path")
    val dir = res.make(new MMapDirectory(Paths.get(jdictPath)))
    val rdr = res.make(DirectoryReader.open(dir))
    new LuceneJmdictImpl(rdr, ec)
  }
}
