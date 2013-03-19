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

package ws.kotonoha.server.records

import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoRecord, MongoMetaRecord}
import net.liftweb.mongodb.record.field.{ObjectIdField, ObjectIdPk}
import net.liftweb.record.field.LongField

/**
 * @author eiennohito
 * @since 18.03.13 
 */

class RecommendationIgnore private() extends MongoRecord[RecommendationIgnore] with ObjectIdPk[RecommendationIgnore] {
  def meta = RecommendationIgnore

  object user extends ObjectIdField(this)
  object jmdict extends LongField(this)
}

object RecommendationIgnore extends RecommendationIgnore with MongoMetaRecord[RecommendationIgnore] with NamedDatabase
