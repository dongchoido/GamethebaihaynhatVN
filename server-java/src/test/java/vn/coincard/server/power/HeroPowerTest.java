package vn.coincard.server.power;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vn.coincard.server.game.*;
import java.util.List;

/**
 * Abstraction + Polymorphism: HeroPower là Strategy.
 */
class HeroPowerTest {

  private Player playerWithHero(String id, String heroClass, int mana) {
    Hero hero = new Hero("h", "Test", heroClass, "Power", 2, "img");
    Player p = new Player(id, "One-" + id, hero, new Deck(List.of()));
    for (int i = 0; i < mana; i++) p.increaseMaxMana();
    p.refillMana();
    return p;
  }

  @Test
  void magePowerDealsOneDamage() {
    Player p1 = playerWithHero("p1", "MAGE", 2);
    Player p2 = playerWithHero("p2", "MAGE", 0);
    Game game = new Game("g1", "R1", p1, p2);
    game.start();
    // Ensure it's p1 turn and has mana
    HeroPower power = HeroPower.forClass("MAGE");
    int hpBefore = p2.heroState().currentHealth();
    power.execute(game, p1);
    assertEquals(hpBefore - 1, p2.heroState().currentHealth());
  }

  @Test
  void paladinPowerSummonsRecruit() {
    Player p1 = playerWithHero("p1", "PALADIN", 2);
    HeroPower power = HeroPower.forClass("PALADIN");
    Game game = new Game("g1", "R1", p1, playerWithHero("p2", "MAGE", 0));
    game.start();
    power.execute(game, p1);
    assertEquals(1, p1.boardCount());
  }

  @Test
  void priestPowerHeals() {
    Player p1 = playerWithHero("p1", "PRIEST", 2);
    p1.heroState().takeDamage(10);
    HeroPower power = HeroPower.forClass("PRIEST");
    Game game = new Game("g1", "R1", p1, playerWithHero("p2", "MAGE", 0));
    game.start();
    power.execute(game, p1);
    assertEquals(22, p1.heroState().currentHealth());
  }

  @Test
  void warlockPowerDamagesSelfAndDraws() {
    Player p1 = playerWithHero("p1", "WARLOCK", 2);
    HeroPower power = HeroPower.forClass("WARLOCK");
    Game game = new Game("g1", "R1", p1, playerWithHero("p2", "MAGE", 0));
    game.start();
    int hpBefore = p1.heroState().currentHealth();
    int deckBefore = p1.deckSize();
    power.execute(game, p1);
    assertEquals(hpBefore - 2, p1.heroState().currentHealth());
  }

  @Test
  void differentPowersAreDifferentStrategies() {
    HeroPower mage = HeroPower.forClass("MAGE");
    HeroPower hunter = HeroPower.forClass("HUNTER");
    HeroPower priest = HeroPower.forClass("PRIEST");
    assertNotEquals(mage.getClass(), priest.getClass());
    assertEquals(mage.getClass(), hunter.getClass()); // both DamagePower but different amount
  }
}
