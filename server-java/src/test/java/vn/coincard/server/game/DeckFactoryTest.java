package vn.coincard.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeckFactoryTest {
  private static CardTypes.CardDefinition card(String slug, HeroClass heroClass,
      Rarity rarity, boolean collectible) {
    return new CardTypes.CardDefinition(slug, slug, slug, "", CardType.MINION, rarity, 1, 1, 1,
        heroClass, "img", List.of(), List.of(), collectible);
  }

  private static List<CardTypes.CardDefinition> catalog() {
    List<CardTypes.CardDefinition> cards = new ArrayList<>();
    for (int i = 0; i < 15; i++) {
      cards.add(card("neutral-" + i, HeroClass.NEUTRAL, Rarity.COMMON, true));
    }
    cards.add(card("mage-card", HeroClass.MAGE, Rarity.COMMON, true));
    cards.add(card("token", HeroClass.MAGE, Rarity.COMMON, false));
    return cards;
  }

  @Test
  void defaultDeckIsExactlyThirtyAndClassLegal() {
    List<String> deck = DeckFactory.defaultDeck(HeroClass.MAGE, catalog());
    assertEquals(30, deck.size());
    assertEquals(30, DeckFactory.validate(HeroClass.MAGE, deck, catalog()).size());
  }

  @Test
  void rejectsTokenAndTooManyCopies() {
    List<String> tokenDeck = new ArrayList<>(DeckFactory.defaultDeck(HeroClass.MAGE, catalog()));
    tokenDeck.set(0, "token");
    assertThrows(GameException.InvalidDeck.class,
        () -> DeckFactory.validate(HeroClass.MAGE, tokenDeck, catalog()));

    List<String> duplicateDeck = new ArrayList<>();
    for (int i = 0; i < 30; i++) duplicateDeck.add("neutral-0");
    assertThrows(GameException.InvalidDeck.class,
        () -> DeckFactory.validate(HeroClass.MAGE, duplicateDeck, catalog()));
  }
}
