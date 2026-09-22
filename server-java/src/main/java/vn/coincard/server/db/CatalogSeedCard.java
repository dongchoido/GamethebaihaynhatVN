package vn.coincard.server.db;

import java.util.List;
import vn.coincard.server.game.CardType;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Keyword;
import vn.coincard.server.game.Rarity;

/** JSON-only adapter model for a seed row that has no persistent card id yet. */
public record CatalogSeedCard(
    String name,
    String slug,
    CardType type,
    Rarity rarity,
    int manaCost,
    int attack,
    int health,
    HeroClass heroClass,
    String imagePath,
    String description,
    List<CardTypes.EffectDefinition> effects,
    List<Keyword> keywords,
    Boolean collectible) {
  public CardTypes.CardDefinition toCardDefinition() {
    return new CardTypes.CardDefinition(
        "card_" + slug,
        name == null ? "" : name,
        slug == null ? "" : slug,
        description == null ? "" : description,
        type,
        rarity,
        manaCost,
        attack,
        health,
        heroClass,
        imagePath == null ? "" : imagePath,
        effects,
        keywords,
        !Boolean.FALSE.equals(collectible));
  }
}
