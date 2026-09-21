package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Server-side shuffled card deck. */
public class Deck {
  private List<CardTypes.CardDefinition> cards;
  private final Random random = new Random();

  public Deck(List<CardTypes.CardDefinition> cards) {
    this.cards = new ArrayList<>(cards);
    shuffle();
  }

  public int size() { return cards.size(); }
  public CardTypes.CardDefinition drawOne() {
    if (cards.isEmpty()) throw new IllegalStateException("Deck is empty.");
    return cards.remove(0);
  }

  public void returnToBottom(CardTypes.CardDefinition card) {
    cards.add(card);
  }

  /** Opaque undo. */
  public Runnable checkpoint() {
    List<CardTypes.CardDefinition> snapshot = new ArrayList<>(cards);
    return () -> cards = new ArrayList<>(snapshot);
  }

  /** Cheapest card costing <= maxCost, or null. */
  public CardTypes.CardDefinition drawCheapest(int maxCost) {
    int best = -1;
    for (int i = 0; i < cards.size(); i++) {
      CardTypes.CardDefinition c = cards.get(i);
      if (c.manaCost() > maxCost) continue;
      if (best == -1 || c.manaCost() < cards.get(best).manaCost()) best = i;
    }
    return best == -1 ? null : cards.remove(best);
  }

  private void shuffle() {
    for (int i = cards.size() - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      Collections.swap(cards, i, j);
    }
  }
}
