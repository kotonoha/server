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

package ws.kotonoha.server.records

import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.field.LongPk
import java.util.concurrent.atomic.AtomicLong
import com.mongodb.BasicDBObject
import net.liftweb.mongodb.Limit
import net.liftweb.record.field.LongField
import org.slf4j.LoggerFactory
import io.Codec

/**
 * @author eiennohito
 * @since 09.05.12
 */

trait SequencedLongId[OwnerType <: MongoRecord[OwnerType]] { self: (MongoRecord[OwnerType] with LongPk[OwnerType]) =>

  private def log = LoggerFactory.getLogger(getClass)

  def resolveMaxId: Long = {
    val dbo = new BasicDBObject()
    val sdbo = new BasicDBObject()
    sdbo.append("_id", -1)
    val all = meta.findAll(dbo, sdbo, Limit(1))
    log.debug("Trying to resolve max id for class " + getClass)
    val n = all match {
      case o :: Nil => o.id.asInstanceOf[LongField[OwnerType]].get
      case _ => 0L
    }
    log.debug("Maximum current Id is " + n)
    n
  }

  private lazy val curId: AtomicLong = synchronized { new AtomicLong(resolveMaxId + 1) }
  private def nextId = {
    curId.getAndIncrement
  }

  override def defaultIdValue = nextId
}
