package org.eiennohito.kotonoha.orm

import org.scalatest.BeforeAndAfterAll
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.dao.{Dao, DaoManager}
import org.eiennohito.kotonoha.utls.DateTimeUtils
import com.j256.ormlite.table.TableUtils
import org.eiennohito.kotonoha.model.learning.{ItemLearning, WordCard, Word}

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


class ModelOrmTest extends org.scalatest.FunSuite with org.scalatest.matchers.ShouldMatchers with BeforeAndAfterAll {
  
  val dbUrl = "jdbc:h2:mem:account"
  val connSrc = new JdbcConnectionSource(dbUrl)  
  val wordDao = DaoManager.createDao(connSrc, classOf[Word]).asInstanceOf[Dao[Word, Long]]
  val cardDao = DaoManager.createDao(connSrc, classOf[WordCard]).asInstanceOf[Dao[Word, Long]]
  
  override def beforeAll {
    TableUtils.createTable(connSrc, classOf[Word])
    TableUtils.createTable(connSrc, classOf[WordCard])
    TableUtils.createTable(connSrc, classOf[ItemLearning])
  }
  
  
  test("Word saves to db") {
    

    val w = new Word
    w.setCreatedOn(DateTimeUtils.now)
    w.setId(5)
    w.setMeaning("mean")
    wordDao.create(w)

    val w2 = wordDao.queryForId(5)
    w2.getMeaning should equal ("mean")
  }
}