package ws.kotonoha.server.web.snippet

import com.google.inject.Inject

import scala.concurrent.ExecutionContext
import xml.NodeSeq

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
/**
 * @author eiennohito
 * @since 03.03.12
 */

class TestSnippet @Inject() (
  ec: ExecutionContext
) {
  def render(x: NodeSeq): NodeSeq =  {
    <em>help me badly2! {ec.toString}</em>
  }
}
