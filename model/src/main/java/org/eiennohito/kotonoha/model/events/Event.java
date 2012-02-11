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
import org.eiennohito.kotonoha.model.Identifiable;
import org.eiennohito.kotonoha.model.ormlite.DateTimePersister;
import org.joda.time.DateTime;

/**
 * @author eiennohito
 * @since 07.02.12
 */
public abstract class Event extends Identifiable {
  @DatabaseField(persisterClass = DateTimePersister.class)
  private DateTime datetime;

  @DatabaseField
  private int eventType = myType();
  
  protected abstract int myType();

  @DatabaseField
  private transient int operation = 0;

  public DateTime getDatetime() {
    return datetime;
  }

  public void setDatetime(DateTime datetime) {
    this.datetime = datetime;
  }

  public int getEventType() {
    return eventType;
  }

  public int getOperation() {
    return operation;
  }

  public void setOperation(int operation) {
    this.operation = operation;
  }
}
