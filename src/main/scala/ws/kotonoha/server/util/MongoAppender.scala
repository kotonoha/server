package ws.kotonoha.server.util

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

import ch.qos.logback.core.AppenderBase
import org.joda.time.DateTime
import ch.qos.logback.classic.spi.{IThrowableProxy, ILoggingEvent}
import ws.kotonoha.server.mongodb.{MongoDbInit, NamedDatabase}
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord, BsonMetaRecord, BsonRecord}
import net.liftweb.mongodb.record.field.{BsonRecordField, ObjectIdPk}
import net.liftweb.record.field.{IntField, EnumField, StringField, DateTimeField}
import net.liftweb.common.{Box, Full, Empty}
import ws.kotonoha.server.records.{JodaDateField, DateJsonFormat}

/**
 * @author eiennohito
 * @since 06.03.12
 */

object LogLevel extends Enumeration(0, "TRACE", "DEBUG", "WARN", "ERROR") {
  type LogLevel = Value

  val TRACE, DEBUG, WARN, ERROR = Value
}


case class LoggingThrowable (classname: String, commonFrames: Int, message: String, cause: Option[LoggingThrowable])

class LoggingThrowableR private() extends BsonRecord[LoggingThrowableR] {
  def meta = LoggingThrowableR

  object classname extends StringField(this, 255)
  object commonFrames extends IntField(this)
  object message extends StringField(this, 500)
  object cause extends BsonRecordField(this, LoggingThrowableR, Empty)
}

object LoggingThrowableR extends LoggingThrowableR with BsonMetaRecord[LoggingThrowableR] {
  def map(tp: IThrowableProxy): Box[LoggingThrowableR] = {
    if (tp == null) {
      Empty
    } else {
      val x = createRecord
      Full(x.classname(tp.getClassName).commonFrames(tp.getCommonFrames).message(tp.getMessage).cause(map(tp.getCause)))
    }
  }
}

class LogRecord private() extends MongoRecord[LogRecord] with ObjectIdPk[LogRecord] {
  def meta = LogRecord

  object timestamp extends JodaDateField(this)
  object thread extends StringField(this, 50)
  object level extends EnumField(this, LogLevel)
  object name extends StringField(this, 50)
  object message extends StringField(this, 1000)
  object throwable extends BsonRecordField(this, LoggingThrowableR, Empty)
}

object LogRecord extends LogRecord with MongoMetaRecord[LogRecord] with NamedDatabase

class MongoAppender extends AppenderBase[ILoggingEvent] {

  def append(ev: ILoggingEvent) {
    import ws.kotonoha.server.util.DateTimeUtils._
    if (!MongoDbInit.inited)
      return

    val obj = LogRecord.createRecord.
      timestamp(new DateTime(ev.getTimeStamp)).
      thread(ev.getThreadName).
      level(LogLevel.withName(ev.getLevel.levelStr)).
      name(ev.getLoggerName).
      message(ev.getFormattedMessage).
      throwable(LoggingThrowableR.map(ev.getThrowableProxy))

    obj.save
  }
}
