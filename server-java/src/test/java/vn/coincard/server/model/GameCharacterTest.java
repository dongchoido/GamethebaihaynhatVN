package vn.coincard.server.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vn.coincard.server.game.Constants;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Minion;

/**
 * Inheritance + Encapsulation: Hero và Minion đều là GameCharacter.
 * Kiểm tra hành vi chung takeDamage/heal/isDead được kế thừa từ base.
 */
class GameCharacterTest {

  @Test
  void heroAndMinionAreGameCharacters() {
    Hero hero = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    Minion minion = new Minion("m1", "c1", "Yeti", 4, 5, "p1", false, "img");
    assertTrue(hero instanceof GameCharacter);
    assertTrue(minion instanceof GameCharacter);
  }

  @Test
  void takeDamageAndHeal() {
    Hero hero = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    hero.takeDamage(10);
    assertEquals(20, hero.currentHealth());
    hero.heal(5);
    assertEquals(25, hero.currentHealth());
    hero.heal(100);
    assertEquals(Constants.MAX_HERO_HEALTH, hero.currentHealth());
  }

  @Test
  void healDoesNotExceedMax() {
    Minion m = new Minion("m1", "c1", "Yeti", 2, 5, "p1", false, "img");
    m.takeDamage(3);
    assertEquals(2, m.currentHealth());
    m.heal(10);
    assertEquals(5, m.maxHealth());
    assertEquals(5, m.currentHealth());
  }

  @Test
  void healthNeverNegative() {
    Hero hero = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    hero.takeDamage(100);
    assertEquals(0, hero.currentHealth());
    assertTrue(hero.isDead());
  }

  @Test
  void negativeDamageThrows() {
    Hero hero = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    assertThrows(IllegalArgumentException.class, () -> hero.takeDamage(-1));
    assertThrows(IllegalArgumentException.class, () -> hero.heal(-1));
  }

  @Test
  void minionStateMutatesThroughBehavior() {
    Minion m = new Minion("m1", "c1", "Yeti", 4, 5, "p1", false, "img");
    m.takeDamage(3);
    m.modifyAttack(2);
    assertEquals(2, m.currentHealth());
    assertEquals(6, m.currentAttack());
  }

  @Test
  void polymorphismViaGameCharacter() {
    GameCharacter hero = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    GameCharacter minion = new Minion("m1", "c1", "Yeti", 2, 2, "p1", false, "img");
    hero.takeDamage(5);
    minion.takeDamage(1);
    assertEquals(25, hero.currentHealth());
    assertEquals(1, minion.currentHealth());
    assertFalse(hero.isDead());
    assertFalse(minion.isDead());
  }
}
