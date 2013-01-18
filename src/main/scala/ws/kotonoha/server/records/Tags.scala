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

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdPk}
import net.liftweb.record.field._
import net.liftweb.common.Empty

/**
 * @author eiennohito
 * @since 08.01.13 
 */

class TagInfo private() extends MongoRecord[TagInfo] with ObjectIdPk[TagInfo] {
  def meta = TagInfo

  object tag extends StringField(this, 100)

  object wiki extends OptionalStringField(this, 100, Empty)

  object usage extends LongField(this, 0)

  object public extends BooleanField(this, false)

}

object TagInfo extends TagInfo with MongoMetaRecord[TagInfo] with NamedDatabase

class TagAlias private() extends MongoRecord[TagAlias] with ObjectIdPk[TagAlias] {
  def meta = TagAlias

  object alias extends StringField(this, 100)

  object tag extends StringField(this, 100)

}

object TagAlias extends TagAlias with MongoMetaRecord[TagAlias] with NamedDatabase

class TagDescription private() extends MongoRecord[TagDescription] with ObjectIdPk[TagDescription] {
  def meta = TagDescription

  object tag extends ObjectIdField(this)

  object locale extends LocaleField(this)

  object descr extends StringField(this, 1000)

}

object TagDescription extends TagDescription with MongoMetaRecord[TagDescription] with NamedDatabase

class UserTagInfo private() extends MongoRecord[UserTagInfo] with ObjectIdPk[UserTagInfo] {
  def meta = UserTagInfo

  object tag extends StringField(this, 100)

  object user extends ObjectIdField(this)

  object usage extends LongField(this)

}

object UserTagInfo extends UserTagInfo with MongoMetaRecord[UserTagInfo] with NamedDatabase

class WordTagInfo private() extends MongoRecord[WordTagInfo] with ObjectIdPk[WordTagInfo] {
  def meta = WordTagInfo

  object tag extends StringField(this, 100)

  object word extends StringField(this, 100)

  object usage extends LongField(this)

  object user extends ObjectIdField(this)

}

object WordTagInfo extends WordTagInfo with MongoMetaRecord[WordTagInfo] with NamedDatabase
