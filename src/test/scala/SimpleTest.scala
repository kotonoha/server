import java.util.Date
import org.eiennohito.kotonoha.mongodb.MongoDbInit
import org.eiennohito.kotonoha.records.ItemLearningDataRecord

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
 * @since 29.01.12 
 */

class SimpleTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers {
  test("whatever") {
    MongoDbInit.init()
    val rec: ItemLearningDataRecord = ItemLearningDataRecord.createRecord
    rec.difficulty(1.2).intervalStart(new Date)
    print(rec.asJSON.toJsCmd)
  }
}