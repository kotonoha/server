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

package ws.kotonoha.server.orm

import org.scalatest.BeforeAndAfterAll
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.dao.{Dao, DaoManager}
import ws.kotonoha.server.util.DateTimeUtils
import com.j256.ormlite.table.TableUtils
import ws.kotonoha.server.model.learning.{Example, ItemLearning, WordCard, Word}
import com.j256.ormlite.db.SqliteDatabaseType

class ModelOrmTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with BeforeAndAfterAll {
  
  val dbUrl = "jdbc:sqlite::memory:"
  val connSrc = new JdbcConnectionSource(dbUrl, new SqliteDatabaseType)
  val wordDao = DaoManager.createDao(connSrc, classOf[Word]).asInstanceOf[Dao[Word, String]]
  val cardDao = DaoManager.createDao(connSrc, classOf[WordCard]).asInstanceOf[Dao[Word, String]]
  
  override def beforeAll {
    TableUtils.createTable(connSrc, classOf[Word])
    TableUtils.createTable(connSrc, classOf[WordCard])
    TableUtils.createTable(connSrc, classOf[ItemLearning])
    TableUtils.createTable(connSrc, classOf[Example])
  }
  
  
  test("Word saves to db") {
    val w = new Word
    w.setCreatedOn(DateTimeUtils.now)
    w.setMeaning("mean")
    wordDao.create(w)

    val w2 = wordDao.queryForId(w.getId)
    w2.getMeaning should equal ("mean")
  }
}
