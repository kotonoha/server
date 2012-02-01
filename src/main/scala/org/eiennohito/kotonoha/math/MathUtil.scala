package org.eiennohito.kotonoha.math

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
 * @since 30.01.12
 */

object MathUtil {
  def round(d: Double, radix: Int) = {
    val x = math.pow(10, radix)
    math.round(d * x) / x
  }
  
  def ofrandom = {
    val a = 0.047
    val b = 0.092
    val p = math.random - 0.5
    val m = -1 / b * math.log (1 - b / a * p.abs)
    val mend = m * p.signum
    (100 + mend) / 100
  }
  
  def dayToMillis(days: Double) = {
    val hours = days * 24
    val mins = hours * 60
    (mins * 60 * 1000).round
  }
}
