package vn.coincard.server.game.effects;

import vn.coincard.server.game.Minion;

/** Attack and health buff strategies. */
public final class BuffEffects {
  private BuffEffects() {}

  public static class BuffAttackEffect implements CardEffect {
    private final int amount;
    public BuffAttackEffect(int amount) { this.amount = amount; }
    @Override
    public void execute(EffectContext context) {
      if (context.target instanceof Minion m) m.modifyAttack(amount);
    }
  }

  public static class BuffHealthEffect implements CardEffect {
    private final int amount;
    public BuffHealthEffect(int amount) { this.amount = amount; }
    @Override
    public void execute(EffectContext context) {
      if (context.target instanceof Minion m) m.modifyHealth(amount);
    }
  }
}
