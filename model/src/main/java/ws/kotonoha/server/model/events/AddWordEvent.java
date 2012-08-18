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

package ws.kotonoha.server.model.events;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import ws.kotonoha.server.model.EventTypes;

/**
 * @author eiennohito
 * @since 12.04.12
 */

@DatabaseTable
public class AddWordEvent extends Event {

  @DatabaseField
  private String writing;

  @DatabaseField
  private String reading;

  @DatabaseField
  private String meaning;

  public String getWriting() {
    return writing;
  }

  public void setWriting(String writing) {
    this.writing = writing;
  }

  public String getReading() {
    return reading;
  }

  public void setReading(String reading) {
    this.reading = reading;
  }

  public String getMeaning() {
    return meaning;
  }

  public void setMeaning(String meaning) {
    this.meaning = meaning;
  }

  @Override
  protected int myType() {
    return EventTypes.ADD;
  }
}
