package vn.coincard.server.game.effects;

import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;

/** Records actual damage only (no overkill), enemy side only. */
public class DamageEffect implements ICardEffect {
  private final int amount;

  public DamageEffect(int amount) { this.amount = amount; }

  @Override
  public void execute(EffectContext context) {
    Object target = context.target;
    if (target instanceof Player p) {
      int actual = p.heroState().takeDamage(amount);
      if (p.id().equals(context.opponent.id())) context.player.recordDamage(actual);
      return;
    }
    if (target instanceof Minion m) {
      int actual = m.takeDamage(amount);
      if (m.ownerId.equals(context.opponent.id())) context.player.recordDamage(actual);
    }
  }
}
