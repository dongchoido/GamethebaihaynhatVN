package vn.coincard.server.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.coincard.server.db.Repositories;
import vn.coincard.server.catalog.CatalogCache;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Constants;
import vn.coincard.server.game.DeckFactory;
import vn.coincard.server.game.HeroClass;

/** Read-only catalog contract used by the deck builder. */
@RestController
public class CatalogController {
  private final CatalogCache catalog;

  public CatalogController(CatalogCache catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/api/game-catalog")
  public GameCatalogResponse catalog() {
    List<Repositories.HeroRecord> heroes = List.copyOf(catalog.heroes());
    List<CardTypes.CardDefinition> cards = catalog.cards().stream()
        .filter(CardTypes.CardDefinition::collectible)
        .toList();
    Map<HeroClass, List<String>> suggested = new LinkedHashMap<>();
    for (Repositories.HeroRecord hero : heroes) {
      suggested.put(hero.heroClass(), DeckFactory.defaultDeck(hero.heroClass(), cards));
    }
    return new GameCatalogResponse(heroes, cards,
        new DeckRules(Constants.DECK_SIZE, 2, 1, List.of(
            HeroClass.MAGE, HeroClass.HUNTER, HeroClass.PALADIN,
            HeroClass.PRIEST, HeroClass.WARLOCK)),
        Map.copyOf(suggested));
  }

  public record GameCatalogResponse(
      List<Repositories.HeroRecord> heroes,
      List<CardTypes.CardDefinition> collectibleCards,
      DeckRules deckRules,
      Map<HeroClass, List<String>> suggestedDecks) {
    public GameCatalogResponse {
      heroes = List.copyOf(heroes);
      collectibleCards = List.copyOf(collectibleCards);
      suggestedDecks = Map.copyOf(suggestedDecks);
    }
  }

  public record DeckRules(int deckSize, int maxCopies, int maxLegendaryCopies,
      List<HeroClass> heroClasses) {
    public DeckRules {
      heroClasses = List.copyOf(heroClasses);
    }
  }
}
