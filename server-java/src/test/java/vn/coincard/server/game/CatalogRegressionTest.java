package vn.coincard.server.game;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.coincard.server.db.CatalogSeedCard;

class CatalogRegressionTest {
  private static List<CardTypes.CardDefinition> catalog;

  @BeforeAll
  static void loadCatalog() throws Exception {
    Path root = Path.of(System.getProperty("user.dir"));
    Path path = root.resolve("data/cards.json");
    if (!Files.exists(path)) path = root.resolve("../data/cards.json").normalize();
    ObjectMapper mapper = new ObjectMapper();
    List<CatalogSeedCard> rows = mapper.readValue(Files.readString(path),
        mapper.getTypeFactory().constructCollectionType(List.class, CatalogSeedCard.class));
    catalog = rows.stream().map(CatalogSeedCard::toCardDefinition).toList();
  }

  private static CardTypes.CardDefinition card(String slug) {
    CardTypes.CardDefinition source = catalog.stream()
        .filter(c -> c.slug().equals(slug)).findFirst().orElseThrow();
    return copy(source, slug + "-" + UUID.randomUUID(), source.effects());
  }

  private static CardTypes.CardDefinition copy(CardTypes.CardDefinition source, String id,
      List<CardTypes.EffectDefinition> effects) {
    return new CardTypes.CardDefinition(id, source.name(), source.slug(), source.description(),
        source.type(), source.rarity(), source.manaCost(), source.attack(), source.health(),
        source.heroClass(), source.imagePath(), effects, source.keywords(), true);
  }

  private Fixture setup() {
    GameTestHarness engine = new GameTestHarness();
    List<CardTypes.CardDefinition> firstDeck = new ArrayList<>();
    List<CardTypes.CardDefinition> secondDeck = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      firstDeck.add(copy(catalog.get(0), "a-" + i, catalog.get(0).effects()));
      secondDeck.add(copy(catalog.get(0), "b-" + i, catalog.get(0).effects()));
    }
    Hero heroA = new Hero("ha", "A", HeroClass.MAGE, "Fireblast", 2, "");
    Hero heroB = new Hero("hb", "B", HeroClass.MAGE, "Fireblast", 2, "");
    Game game = engine.createGame("test-" + UUID.randomUUID(), "ABC123",
        new GameFactory.PlayerInit("a", "A", heroA, new Deck(firstDeck)),
        new GameFactory.PlayerInit("b", "B", heroB, new Deck(secondDeck)));
    GameTestAccess.start(game, "a");
    Player a = game.getPlayerById("a");
    Player b = game.getPlayerById("b");
    for (int i = 0; i < Constants.MAX_MANA; i++) a.increaseMaxMana();
    a.refillMana();
    return new Fixture(engine, game, a, b);
  }

  private Minion unit(Player player, String id, int attack) {
    Minion minion = new Minion(id, id, id, attack, 8, player.id(), true, "");
    player.summonMinion(minion);
    return minion;
  }

  @Test
  void everyCatalogSpellResolvesWithAValidTarget() {
    for (CardTypes.CardDefinition source : catalog.stream()
        .filter(c -> c.type() == CardType.SPELL).toList()) {
      Fixture f = setup();
      Minion own = unit(f.a, "own", 8);
      Minion enemy = unit(f.b, "enemy", 8);
      CardTypes.CardDefinition spell = copy(source, source.slug(), source.effects());
      f.a.addToHand(spell);
      CardTypes.EffectDefinition targeted = spell.effects().stream()
          .filter(e -> CardTypes.effectNeedsTarget(e.target())).findFirst().orElse(null);
      String target = null;
      if (targeted != null) {
        target = switch (targeted.target()) {
          case FRIENDLY_MINION -> own.getInstanceId();
          case FRIENDLY_HERO, FRIENDLY_CHARACTER, SELF -> f.a.id();
          case ENEMY_HERO, ENEMY_CHARACTER -> f.b.id();
          default -> enemy.getInstanceId();
        };
      }
      String targetId = target;
      assertDoesNotThrow(() -> f.engine.playCard(f.game.getGameId(), f.a.id(), spell.id(), targetId),
          source.slug());
      assertEquals(1, f.a.cardsPlayed(), source.slug());
    }
  }

  @Test
  void polymorphReplacesOnAFullBoardWithoutCountingASummon() {
    Fixture f = setup();
    for (int i = 0; i < Constants.MAX_BOARD_SIZE; i++) unit(f.b, "enemy-" + i, 2);
    CardTypes.CardDefinition spell = card("polymorph");
    f.a.addToHand(spell);
    f.engine.playCard(f.game.getGameId(), f.a.id(), spell.id(), "enemy-3");
    assertEquals(Constants.MAX_BOARD_SIZE, f.b.boardCount());
    assertEquals(Constants.MAX_BOARD_SIZE, f.b.minionsSummoned());
    assertEquals(1, f.b.getBoard().get(3).currentHealth());
  }

  @Test
  void conditionalDestroyRejectsBeforeMutation() {
    Fixture f = setup();
    unit(f.b, "target", 1);
    CardTypes.CardDefinition source = card("siphon-soul");
    CardTypes.CardDefinition spell = copy(source, "conditional", List.of(
        new CardTypes.EffectDefinition(
            EffectType.DESTROY, 0, EffectTarget.ENEMY_MINION, null, 5, null)));
    f.a.addToHand(spell);
    int hand = f.a.handCount();
    assertThrows(GameException.InvalidTarget.class,
        () -> f.engine.playCard(f.game.getGameId(), f.a.id(), spell.id(), "target"));
    assertEquals(hand, f.a.handCount());
    assertEquals(10, f.a.currentMana());
    assertEquals(1, f.b.boardCount());
  }

  @Test
  void laterEffectFailureRollsBackBoardHandManaAndStats() {
    Fixture f = setup();
    Minion original = unit(f.b, "target", 2);
    CardTypes.CardDefinition source = card("polymorph");
    CardTypes.CardDefinition spell = copy(source, "rollback", List.of(
        new CardTypes.EffectDefinition(
            EffectType.TRANSFORM, 0, EffectTarget.ANY_MINION, null, null, null),
        new CardTypes.EffectDefinition(
            EffectType.DAMAGE, 2, EffectTarget.ANY_MINION, null, null, null)));
    f.a.addToHand(spell);
    int hand = f.a.handCount();
    assertThrows(GameException.InvalidTarget.class,
        () -> f.engine.playCard(f.game.getGameId(), f.a.id(), spell.id(), "target"));
    assertEquals(hand, f.a.handCount());
    assertEquals(10, f.a.currentMana());
    assertEquals(0, f.a.cardsPlayed());
    assertSame(original, f.b.getBoard().get(0));
    assertEquals(8, original.currentHealth());
  }

  @Test
  void combatStatisticsUseActualDamageWithoutOverkill() {
    Fixture f = setup();
    unit(f.a, "attacker", 9);
    unit(f.b, "defender", 8);
    f.engine.attack(f.game.getGameId(), f.a.id(), "attacker", "defender");
    assertEquals(8, f.a.damageDealt());
    assertEquals(8, f.b.damageDealt());
  }

  private record Fixture(GameTestHarness engine, Game game, Player a, Player b) {}
}
