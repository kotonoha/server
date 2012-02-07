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
package org.eiennohito.kotonoha.model.learning;

import org.joda.time.DateTime;

/**
 * @author eiennohito
 * @since 07.02.12
 */
public class ItemLearning {
  private DateTime intervalStart;
  private DateTime intervalEnd;
  private DateTime intervalLength;
  private double difficulty;
  private int lapse;
  private int repetition;

  public DateTime getIntervalStart() {
    return intervalStart;
  }

  public void setIntervalStart(DateTime intervalStart) {
    this.intervalStart = intervalStart;
  }

  public DateTime getIntervalEnd() {
    return intervalEnd;
  }

  public void setIntervalEnd(DateTime intervalEnd) {
    this.intervalEnd = intervalEnd;
  }

  public DateTime getIntervalLength() {
    return intervalLength;
  }

  public void setIntervalLength(DateTime intervalLength) {
    this.intervalLength = intervalLength;
  }

  public double getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(double difficulty) {
    this.difficulty = difficulty;
  }

  public int getLapse() {
    return lapse;
  }

  public void setLapse(int lapse) {
    this.lapse = lapse;
  }

  public int getRepetition() {
    return repetition;
  }

  public void setRepetition(int repetition) {
    this.repetition = repetition;
  }
}
