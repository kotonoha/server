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

package org.eiennohito.kotonoha.records

import org.eiennohito.kotonoha.mongodb.NamedDatabase
import net.liftweb.mongodb.record.field.LongPk
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.record.field.{DateTimeField, StringField}
import org.eiennohito.kotonoha.util.DateTimeUtils._
import org.joda.time.DateTime
import net.liftweb.oauth.{OAuthNonce, OAuthNonceMeta}

/**
 * @author eiennohito
 * @since 31.03.12
 */

class NonceRecord private() extends MongoRecord[NonceRecord] with LongPk[NonceRecord] {
  def meta = NonceRecord

  object consumerKey extends StringField(this, 32)
  object token extends StringField(this, 32)
  object nonce extends StringField(this, 50)
  object timestamp extends DateTimeField(this)

  def value = new OAuthNonce {
    def nonce = NonceRecord.this.nonce.is

    def consumerKey =  NonceRecord.this.consumerKey.is

    def token =  NonceRecord.this.token.is

    def timestamp =  {
      val cal : DateTime = NonceRecord.this.timestamp.is
      cal.withZone(UTC).getMillis
    }
  }
}

object NonceRecord extends NonceRecord with MongoMetaRecord[NonceRecord] with NamedDatabase with OAuthNonceMeta {
  import com.foursquare.rogue.Rogue._
  def create(consumerKey: String, token: String, timestamp: Long, nonce: String) {
    val rec = createRecord.
      consumerKey(consumerKey).token(token).timestamp(new DateTime(timestamp, UTC)).nonce(nonce)
    rec.save
  }

  def find(key: String, token: String, timestamp: Long, nonce: String) = {

    val q = this where (_.consumerKey eqs key) and (_.token eqs token) and (_.timestamp eqs new DateTime(timestamp, UTC)) and
      (_.nonce eqs nonce)
    q.get() map {_.value}
  }

  def bulkDelete_!!(minTimestamp: Long) {
    val t = new DateTime(minTimestamp, UTC)
    val q = this where (_.timestamp before t)
    q.bulkDelete_!!()
  }
}
