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
 * @since 07.02.12
 */
@DatabaseTable
public class MarkEvent extends Event {

  @DatabaseField
  private long card;
  @DatabaseField
  private int mode;
  @DatabaseField
  private double mark;
  @DatabaseField
  private double time;
  
  @Override
  protected int myType() {
    return EventTypes.MARK;
  }

  public long getCard() {
    return card;
  }

  public void setCard(long card) {
    this.card = card;
  }

  public int getMode() {
    return mode;
  }

  public void setMode(int mode) {
    this.mode = mode;
  }

  public double getMark() {
    return mark;
  }

  public void setMark(double mark) {
    this.mark = mark;
  }

  public double getTime() {
    return time;
  }

  public void setTime(double time) {
    this.time = time;
  }
}
