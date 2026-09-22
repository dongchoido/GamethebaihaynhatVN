package vn.coincard.server.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.CardType;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.GameTestAccess;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.HeroPower;
import vn.coincard.server.game.HeroPowerRegistry;
import vn.coincard.server.game.Player;
import vn.coincard.server.game.Rarity;

/**
 * Abstraction + Polymorphism: HeroPower là Strategy.
 */
class HeroPowerTest {

  private Player playerWithHero(String id, HeroClass heroClass, int mana) {
    Hero hero = new Hero("h", "Test", heroClass, "Power", 2, "img");
    Player p = new Player(id, "One-" + id, hero, new Deck(testDeck()));
    for (int i = 0; i < mana; i++) p.increaseMaxMana();
    p.refillMana();
    return p;
  }

  @Test
  void magePowerDealsOneDamage() {
    Player p1 = playerWithHero("p1", HeroClass.MAGE, 2);
    Player p2 = playerWithHero("p2", HeroClass.MAGE, 0);
    Game game = game(p1, p2);
    GameTestAccess.start(game, "p1");
    // Ensure it's p1 turn and has mana
    HeroPower power = powerFor(HeroClass.MAGE);
    int hpBefore = p2.heroState().currentHealth();
    power.execute(game, p1);
    assertEquals(hpBefore - 1, p2.heroState().currentHealth());
  }

  @Test
  void paladinPowerSummonsRecruit() {
    Player p1 = playerWithHero("p1", HeroClass.PALADIN, 2);
    HeroPower power = powerFor(HeroClass.PALADIN);
    Game game = game(p1, playerWithHero("p2", HeroClass.MAGE, 0));
    GameTestAccess.start(game, "p1");
    power.execute(game, p1);
    assertEquals(1, p1.boardCount());
  }

  @Test
  void priestPowerHeals() {
    Player p1 = playerWithHero("p1", HeroClass.PRIEST, 2);
    p1.heroState().takeDamage(10);
    HeroPower power = powerFor(HeroClass.PRIEST);
    Game game = game(p1, playerWithHero("p2", HeroClass.MAGE, 0));
    GameTestAccess.start(game, "p1");
    power.execute(game, p1);
    assertEquals(22, p1.heroState().currentHealth());
  }

  @Test
  void warlockPowerDamagesSelfAndDraws() {
    Player p1 = playerWithHero("p1", HeroClass.WARLOCK, 2);
    HeroPower power = powerFor(HeroClass.WARLOCK);
    Game game = game(p1, playerWithHero("p2", HeroClass.MAGE, 0));
    GameTestAccess.start(game, "p1");
    int hpBefore = p1.heroState().currentHealth();
    power.execute(game, p1);
    assertEquals(hpBefore - 3, p1.heroState().currentHealth());
  }

  @Test
  void differentPowersAreDifferentStrategies() {
    HeroPower mage = powerFor(HeroClass.MAGE);
    HeroPower hunter = powerFor(HeroClass.HUNTER);
    HeroPower priest = powerFor(HeroClass.PRIEST);
    assertNotEquals(mage.getClass(), priest.getClass());
    assertEquals(mage.getClass(), hunter.getClass()); // both DamagePower but different amount
  }

  private HeroPower powerFor(HeroClass heroClass) {
    return new HeroPowerRegistry(HeroPower.defaults()).forClass(heroClass);
  }

  private Game game(Player first, Player second) {
    return GameTestAccess.create("g1", "R1", first, second);
  }

  private List<CardTypes.CardDefinition> testDeck() {
    return List.of(card("d1"), card("d2"), card("d3"), card("d4"));
  }

  private CardTypes.CardDefinition card(String id) {
    return new CardTypes.CardDefinition(id, id, id, "", CardType.MINION, Rarity.COMMON,
        1, 1, 1, HeroClass.NEUTRAL, "img", List.of(), List.of(), true);
  }
}
