package vn.coincard.server.combat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.CombatService;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;

class CombatServiceTest {

  private Player player(String id) {
    Hero hero = new Hero("h", "Test", HeroClass.MAGE, "Power", 2, "img");
    return new Player(id, id, hero, new Deck(List.of()));
  }

  @Test
  void normalAttackExchangeDamage() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion a = new Minion("a1", "c1", "A", 3, 3, "p1", true, "img");
    Minion d = new Minion("d1", "c2", "D", 2, 2, "p2", false, "img");
    p1.summonMinion(a);
    p2.summonMinion(d);
    CombatService.resolveAttack(p1, p2, "a1", "d1");
    assertTrue(a.isDead() || d.isDead() || a.currentHealth() == 1);
    // Both took damage
    assertEquals(1, a.currentHealth()); // 3-2
    assertTrue(d.isDead()); // 2-3 = -1
  }

  @Test
  void nonTauntMinionsDoNotBlockHero() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion a = new Minion("a1", "c1", "A", 3, 3, "p1", true, "img");
    Minion b = new Minion("b1", "c2", "B", 2, 2, "p2", false, "img");
    p1.summonMinion(a);
    p2.summonMinion(b);
    assertDoesNotThrow(() -> CombatService.resolveAttack(p1, p2, "a1", "p2"));
    assertEquals(27, p2.heroState().currentHealth());
  }

  @Test
  void tauntMustBeAttackedFirst() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion a = new Minion("a1", "c1", "A", 3, 3, "p1", true, "img");
    Minion taunt = new Minion("t1", "c2", "T", 1, 1, "p2", false, "img", true);
    Minion other = new Minion("o1", "c3", "O", 1, 1, "p2", false, "img");
    p1.summonMinion(a);
    p2.summonMinion(taunt);
    p2.summonMinion(other);
    assertThrows(GameException.TauntRequired.class,
        () -> CombatService.resolveAttack(p1, p2, "a1", "o1"));
    assertDoesNotThrow(() -> CombatService.resolveAttack(p1, p2, "a1", "t1"));
  }

  @Test
  void summoningSicknessCannotAttack() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion a = new Minion("a1", "c1", "A", 3, 3, "p1", false, "img"); // no charge
    p1.summonMinion(a);
    assertThrows(GameException.InvalidTarget.class,
        () -> CombatService.resolveAttack(p1, p2, "a1", "p2"));
  }

  @Test
  void attackHeroRecordsDamage() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion a = new Minion("a1", "c1", "A", 5, 5, "p1", true, "img");
    p1.summonMinion(a);
    CombatService.resolveAttack(p1, p2, "a1", "p2");
    assertEquals(5, p1.damageDealt());
    assertEquals(25, p2.heroState().currentHealth());
  }
}
