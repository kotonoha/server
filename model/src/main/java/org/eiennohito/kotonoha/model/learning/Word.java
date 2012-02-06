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

import com.google.gson.annotations.SerializedName;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author eiennohito
 * @since 06.02.12
 */
public class Word {
  @SerializedName("_id")
  private long id;

  private String writing;
  private String reading;
  private String meaning;
  
  private DateTime createdOn;
  
  private Collection<Example> examples = new ArrayList<Example>();

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

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

  public DateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(DateTime createdOn) {
    this.createdOn = createdOn;
  }

  public Collection<Example> getExamples() {
    return examples;
  }

  public void setExamples(Collection<Example> examples) {
    this.examples = examples;
  }
}
