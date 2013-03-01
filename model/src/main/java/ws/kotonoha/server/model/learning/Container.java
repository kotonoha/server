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

import java.util.Collection;

/**
 * @author eiennohito
 * @since 07.02.12
 */
public class Container {
  private Collection<Word> words;
  private Collection<WordCard> cards;
  private Collection<ReviewCard> sequence;

  public Collection<Word> getWords() {
    return words;
  }

  public void setWords(Collection<Word> words) {
    this.words = words;
  }

  public Collection<WordCard> getCards() {
    return cards;
  }

  public void setCards(Collection<WordCard> cards) {
    this.cards = cards;
  }

  public Collection<ReviewCard> getSequence() {
    return sequence;
  }

  public void setSequence(Collection<ReviewCard> sequence) {
    this.sequence = sequence;
  }
}
