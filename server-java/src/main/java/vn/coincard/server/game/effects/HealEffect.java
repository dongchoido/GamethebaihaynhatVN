package vn.coincard.server.game.effects;

import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;

public class HealEffect implements ICardEffect {
  private final int amount;

  public HealEffect(int amount) { this.amount = amount; }

  @Override
  public void execute(EffectContext context) {
    Object target = context.target;
    if (target instanceof Player p) {
      p.heroState().heal(amount);
    } else if (target instanceof Minion m) {
      m.heal(amount);
    }
  }
}
