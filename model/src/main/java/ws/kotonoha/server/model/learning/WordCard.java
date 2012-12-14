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
package ws.kotonoha.server.model.learning;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import ws.kotonoha.server.model.Identifiable;
import ws.kotonoha.server.model.ormlite.DateTimePersister;
import org.joda.time.DateTime;

/**
 * @author eiennohito
 * @since 07.02.12
 */
@DatabaseTable
public class WordCard extends Identifiable {
  @DatabaseField
  private int cardMode;
  @DatabaseField(persisterClass = DateTimePersister.class)
  private DateTime createdOn;
  @DatabaseField(persisterClass = DateTimePersister.class)
  private DateTime notBefore;
  @DatabaseField(persisterClass = DateTimePersister.class)
  private transient DateTime gotOn;
  @DatabaseField
  private String word;
  @DatabaseField(foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true, canBeNull = true)
  private ItemLearning learning;
  @DatabaseField
  private transient int status = 0;
  private transient boolean enabled;

  public int getCardMode() {
    return cardMode;
  }

  public void setCardMode(int cardMode) {
    this.cardMode = cardMode;
  }

  public DateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(DateTime createdOn) {
    this.createdOn = createdOn;
  }

  public DateTime getNotBefore() {
    return notBefore;
  }

  public void setNotBefore(DateTime notBefore) {
    this.notBefore = notBefore;
  }

  public ItemLearning getLearning() {
    return learning;
  }

  public void setLearning(ItemLearning learning) {
    this.learning = learning;
  }

  public String getWord() {
    return word;
  }

  public void setWord(String word) {
    this.word = word;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public DateTime getGotOn() {
    return gotOn;
  }

  public void setGotOn(DateTime gotOn) {
    this.gotOn = gotOn;
  }
}
