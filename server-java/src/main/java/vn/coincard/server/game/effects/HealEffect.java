package vn.coincard.server.game.effects;

import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Hero;

public class HealEffect implements CardEffect {
  private final int amount;

  public HealEffect(int amount) { this.amount = amount; }

  @Override
  public void execute(EffectContext context) {
    if (context.target instanceof Hero hero) {
      hero.heal(amount);
    } else if (context.target instanceof Minion m) {
      m.heal(amount);
    }
  }
}
