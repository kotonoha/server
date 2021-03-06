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
import net.liftweb.mongodb.record.field.ObjectIdPk
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.record.field.StringField
import ws.kotonoha.server.util.DateTimeUtils._
import org.joda.time.DateTime
import net.liftmodules.oauth.{OAuthNonce, OAuthNonceMeta}
import com.mongodb.WriteConcern
import ws.kotonoha.server.records.meta.{JodaDateField, KotonohaMongoRecord}

/**
 * @author eiennohito
 * @since 31.03.12
 */

class NonceRecord private() extends MongoRecord[NonceRecord] with ObjectIdPk[NonceRecord] {
  def meta = NonceRecord

  object consumerKey extends StringField(this, 32)

  object token extends StringField(this, 32)

  object nonce extends StringField(this, 50)

  object timestamp extends JodaDateField(this)

  def value = new OAuthNonce {
    def nonce = NonceRecord.this.nonce.get

    def consumerKey = NonceRecord.this.consumerKey.get

    def token = NonceRecord.this.token.get

    def timestamp = {
      val cal: DateTime = NonceRecord.this.timestamp.get
      cal.withZone(UTC).getMillis
    }
  }
}

object NonceRecord extends NonceRecord with MongoMetaRecord[NonceRecord] with NamedDatabase with OAuthNonceMeta with KotonohaMongoRecord[NonceRecord] {

  import ws.kotonoha.server.mongodb.KotonohaLiftRogue._

  def create(consumerKey: String, token: String, timestamp: Long, nonce: String) {
    val rec = createRecord.
      consumerKey(consumerKey).token(token).timestamp(new DateTime(timestamp, UTC)).nonce(nonce)
    rec.save()
  }

  def find(key: String, token: String, timestamp: Long, nonce: String) = {

    val d = new DateTime(timestamp, UTC)
    val q = this where (_.consumerKey eqs key) and (_.token eqs token) and (_.timestamp between(d.minusSeconds(5), d.plusSeconds(5))) and
      (_.nonce eqs nonce)
    q.get() map {
      _.value
    }
  }

  def bulkDelete_!!(minTimestamp: Long) {
    val t = new DateTime(minTimestamp, UTC)
    val q = this where (_.timestamp lt t)
    q.bulkDelete_!!(WriteConcern.NORMAL)
  }
}
