package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.List;

/** Server-side shuffled card deck. */
public class Deck {
  private List<CardTypes.CardDefinition> cards;
  private final RandomSource randomSource;

  public Deck(List<CardTypes.CardDefinition> cards) {
    this(cards, RandomSource.threadLocal());
  }

  public Deck(List<CardTypes.CardDefinition> cards, RandomSource randomSource) {
    this.cards = new ArrayList<>(cards);
    this.randomSource = randomSource;
    shuffle();
  }

  public int size() { return cards.size(); }
  public CardTypes.CardDefinition drawOne() {
    if (cards.isEmpty()) throw new IllegalStateException("Deck is empty.");
    return cards.remove(0);
  }

  record State(List<CardTypes.CardDefinition> cards) {
    State {
      cards = List.copyOf(cards);
    }
  }

  State snapshotState() {
    return new State(cards);
  }

  void restoreState(State state) {
    cards = new ArrayList<>(state.cards());
  }

  private void shuffle() {
    randomSource.shuffle(cards);
  }
}
