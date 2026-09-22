package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds and validates 30-card class decks. */
public final class DeckFactory {
  private DeckFactory() {}

  public static List<String> defaultDeck(HeroClass heroClass, List<CardTypes.CardDefinition> catalog) {
    List<CardTypes.CardDefinition> eligible = catalog.stream()
        .filter(CardTypes.CardDefinition::collectible)
        .filter(heroClass::canUse)
        .sorted(Comparator.comparingInt(CardTypes.CardDefinition::manaCost)
            .thenComparing(CardTypes.CardDefinition::rarity)
            .thenComparing(CardTypes.CardDefinition::slug))
        .toList();
    List<String> result = new ArrayList<>();
    for (CardTypes.CardDefinition card : eligible) {
      int copies = card.rarity().copyLimit();
      for (int i = 0; i < copies && result.size() < Constants.DECK_SIZE; i++) {
        result.add(card.slug());
      }
    }
    if (result.size() != Constants.DECK_SIZE) {
      throw new GameException.InvalidDeck("Catalog không đủ lá hợp lệ cho hero " + heroClass + ".");
    }
    return List.copyOf(result);
  }

  public static List<CardTypes.CardDefinition> validate(HeroClass heroClass,
      List<String> slugs, List<CardTypes.CardDefinition> catalog) {
    if (slugs == null || slugs.size() != Constants.DECK_SIZE) {
      throw new GameException.InvalidDeck("Deck phải có đúng 30 lá.");
    }
    Map<String, CardTypes.CardDefinition> bySlug = new HashMap<>();
    for (CardTypes.CardDefinition card : catalog) {
      if (card.collectible()) bySlug.put(card.slug(), card);
    }
    Map<String, Integer> counts = new HashMap<>();
    List<CardTypes.CardDefinition> result = new ArrayList<>();
    for (String slug : slugs) {
      CardTypes.CardDefinition card = bySlug.get(slug);
      if (card == null) throw new GameException.InvalidDeck("Lá không hợp lệ: " + slug);
      if (!heroClass.canUse(card)) {
        throw new GameException.InvalidDeck("Lá " + slug + " không thuộc hero class.");
      }
      int count = counts.merge(slug, 1, Integer::sum);
      int limit = card.rarity().copyLimit();
      if (count > limit) throw new GameException.InvalidDeck("Lá " + slug + " vượt quá giới hạn bản sao.");
      result.add(card);
    }
    return List.copyOf(result);
  }
}
