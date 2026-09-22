package vn.coincard.server.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.effects.DamageEffect;
import vn.coincard.server.game.effects.HealEffect;
import vn.coincard.server.game.effects.AreaEffects;
import vn.coincard.server.game.effects.BuffEffects;
import vn.coincard.server.game.effects.EffectContext;
import java.util.List;

/**
 * Strategy + Polymorphism: mỗi EffectStrategy tự xử lý behavior.
 */
class EffectStrategyTest {

  private Player player(String id) {
    Hero hero = new Hero("h", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    return new Player(id, id, hero, new Deck(List.of()));
  }

  @Test
  void damageEffectShouldDamageMinion() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion target = new Minion("m1", "c1", "Yeti", 2, 5, "p2", false, "img");
    p2.getBoard(); // just to ensure board exists
    p2.summonMinion(target);
    DamageEffect effect = new DamageEffect(3);
    effect.execute(new EffectContext(p1, p2, target, null));
    assertEquals(2, target.currentHealth());
  }

  @Test
  void healEffectShouldHealHero() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    p1.heroState().takeDamage(10);
    HealEffect effect = new HealEffect(5);
    effect.execute(new EffectContext(p1, p2, p1.heroState(), null));
    assertEquals(25, p1.heroState().currentHealth());
  }

  @Test
  void destroyEffectShouldKillMinion() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion target = new Minion("m1", "c1", "Big", 6, 6, "p2", false, "img");
    p2.summonMinion(target);
    AreaEffects.DestroyEffect effect = new AreaEffects.DestroyEffect(5);
    effect.execute(new EffectContext(p1, p2, target, null));
    assertTrue(target.isDead());
  }

  @Test
  void buffAttackEffect() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion m = new Minion("m1", "c1", "Yeti", 2, 2, "p1", false, "img");
    p1.summonMinion(m);
    BuffEffects.BuffAttackEffect effect = new BuffEffects.BuffAttackEffect(2);
    effect.execute(new EffectContext(p1, p2, m, null));
    assertEquals(4, m.currentAttack());
  }

  @Test
  void transformEffectReplacesWithSheep() {
    Player p1 = player("p1");
    Player p2 = player("p2");
    Minion target = new Minion("m1", "c1", "Yeti", 4, 5, "p2", false, "img");
    p2.summonMinion(target);
    AreaEffects.TransformEffect effect = new AreaEffects.TransformEffect();
    effect.execute(new EffectContext(p1, p2, target, null));
    assertEquals(1, p2.getBoard().get(0).currentHealth());
    assertEquals(1, p2.getBoard().get(0).currentAttack());
  }
}
