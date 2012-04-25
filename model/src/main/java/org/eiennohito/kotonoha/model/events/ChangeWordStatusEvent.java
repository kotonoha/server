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

package org.eiennohito.kotonoha.model.events;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.eiennohito.kotonoha.model.EventTypes;
import org.joda.time.DateTime;

/**
 * @author eiennohito
 * @since 24.04.12
 */
@DatabaseTable(tableName = "ChangeWordStatusEvent")
public class ChangeWordStatusEvent extends Event {

  ChangeWordStatusEvent(Long id, int status) {
    super();
    wordId = id;
    toStatus = status;
    setDatetime(DateTime.now());
  }

  private static final int CHECK_WORD = 2;
  private static final int CHECK_EXAMPLE = 3;

  @DatabaseField
  private Long wordId;

  @DatabaseField
  private Integer toStatus;


  @Override
  protected int myType() {
    return EventTypes.CHANGE_WORD_STATUS;
  }

  public void setWordId(Long wordId) {
    this.wordId = wordId;
  }

  public void setToStatus(Integer toStatus) {
    this.toStatus = toStatus;
  }

  public Long getWordId() {
    return wordId;
  }

  public Integer getToStatus() {
    return toStatus;
  }

  public static ChangeWordStatusEvent checkWord(Long id) {
    return new ChangeWordStatusEvent(id, CHECK_WORD);
  }


}
