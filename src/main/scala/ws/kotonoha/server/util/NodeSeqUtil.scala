/*
 * Copyright 2012-2016 eiennohito (Tolmachev Arseny)
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

import scala.xml.NodeSeq

/**
 * @author eiennohito
 * @since 12.03.13 
 */

object NodeSeqUtil {

  type NodeSeqFn = NodeSeq => NodeSeq

  def mixSeq[T, S](seq: TraversableOnce[T], sep: S)(implicit cv1: T => NodeSeq, cv2: S => NodeSeq): NodeSeq = {
    val buf = NodeSeq.newBuilder
    var first = true
    val sep0: NodeSeq = cv2(sep)
    for (x <- seq) {
      val cvt = cv1(x)
      if (first) {
        first = false
        buf ++= cvt
      } else {
        buf ++= sep0
        buf ++= cvt
      }
    }
    buf.result()
  }

  def transSeq[T](seq: TraversableOnce[T], sep: NodeSeq = NodeSeq.Empty)(f: T => NodeSeq): NodeSeq = {
    implicit val tf = f
    mixSeq(seq, sep)
  }
}
