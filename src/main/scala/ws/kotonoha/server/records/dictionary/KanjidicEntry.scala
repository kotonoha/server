
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

package ws.kotonoha.server.records.dictionary

import ws.kotonoha.akane.dict.kanjidic2.{RmGroup, Misc, Entry, KanjidicEntry}
import ws.kotonoha.server.mongodb.NamedDatabase
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.mongodb.record.field.{MongoListField, MongoCaseClassField, MongoCaseClassListField, ObjectIdPk}
import net.liftweb.record.field.StringField

/**
 * @author eiennohito
 * @since 18.01.13 
 */

class KanjidicRecord extends MongoRecord[KanjidicRecord] with ObjectIdPk[KanjidicRecord] {
  def meta = KanjidicRecord

  object literal extends StringField(this, 3)

  object codepoints extends MongoCaseClassListField[KanjidicRecord, Entry](this)

  object radicals extends MongoCaseClassListField[KanjidicRecord, Entry](this)

  object misc extends MongoCaseClassField[KanjidicRecord, Misc](this)

  object codes extends MongoCaseClassListField[KanjidicRecord, Entry](this)

  object rmgroups extends MongoCaseClassListField[KanjidicRecord, RmGroup](this)

  object nanori extends MongoListField[KanjidicRecord, String](this)

  def fromKe(e: KanjidicEntry) = {
    KanjidicRecord.createRecord
      .literal(e.literal)
      .codepoints(e.codepoints)
      .radicals(e.radicals)
      .misc(e.misc)
      .codes(e.codes)
      .rmgroups(e.rmgroups)
      .nanori(e.nanori)
  }
}

object KanjidicRecord extends KanjidicRecord with MongoMetaRecord[KanjidicRecord] with NamedDatabase
