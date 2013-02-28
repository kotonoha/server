/*
 * Copyright 2012-2013 eiennohito
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

/**
 * @author eiennohito
 * @since 27.02.13 
 */
class Aggregator(val sum: Double, val count: Long, val mean: Double, m2: Double) {
  def this() = this(0, 0, 0, 0)

  def apply(x: Double) = {
    val n = count + 1
    val delta = x - mean
    val nmean = mean + delta / n
    val nm2 = m2 + delta * (x - nmean)
    new Aggregator(sum + x, n, nmean, nm2)
  }

  def variance = m2 / (count - 1)
}
