package vn.coincard.server.game.effects;

import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Hero;

/** Records actual damage only (no overkill), enemy side only. */
public class DamageEffect implements CardEffect {
  private final int amount;

  public DamageEffect(int amount) { this.amount = amount; }

  @Override
  public void execute(EffectContext context) {
    if (context.target instanceof Hero hero) {
      int actual = hero.takeDamage(amount);
      if (hero == context.opponent.heroState()) context.player.recordDamage(actual);
      return;
    }
    if (context.target instanceof Minion m) {
      int actual = m.takeDamage(amount);
      if (m.getOwnerId().equals(context.opponent.id())) context.player.recordDamage(actual);
    }
  }
}
