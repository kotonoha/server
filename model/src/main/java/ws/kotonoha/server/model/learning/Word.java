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
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import ws.kotonoha.server.model.Identifiable;
import ws.kotonoha.server.model.ormlite.DateTimePersister;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author eiennohito
 * @since 06.02.12
 */
@DatabaseTable
public class Word extends Identifiable {
  @DatabaseField
  private String writing;
  @DatabaseField
  private String reading;
  @DatabaseField
  private String meaning;
  @DatabaseField(persisterClass = DateTimePersister.class)
  private DateTime createdOn;
  @ForeignCollectionField(eager = true, orderColumnName = "example")
  private Collection<Example> examples = new ArrayList<Example>();
  private transient Integer status;
  private Collection<String> tags = new ArrayList<String>();

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

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }
}
