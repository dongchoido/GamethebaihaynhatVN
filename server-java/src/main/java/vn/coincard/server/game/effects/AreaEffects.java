package vn.coincard.server.game.effects;

import java.util.List;
import java.util.UUID;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;
import vn.coincard.server.game.TokenCards;

/** Mirror of AoeEffects.ts */
public final class AreaEffects {
  private AreaEffects() {}

  public static class TwistingNetherEffect implements ICardEffect {
    @Override
    public void execute(EffectContext context) {
      for (Minion m : context.player.getBoard()) m.takeDamage(m.currentHealth());
      for (Minion m : context.opponent.getBoard()) m.takeDamage(m.currentHealth());
    }
  }

  public static class AoeDamageEffect implements ICardEffect {
    private final int amount;
    public AoeDamageEffect(int amount) { this.amount = amount; }
    @Override
    public void execute(EffectContext context) {
      List<Minion> targets = context.areaTargets != null
          ? context.areaTargets : context.opponent.getBoard();
      for (Minion m : targets) {
        int actual = m.takeDamage(amount);
        if (m.ownerId.equals(context.opponent.id())) context.player.recordDamage(actual);
      }
    }
  }

  public static class DestroyEffect implements ICardEffect {
    private final int minAttack;
    public DestroyEffect(int minAttack) { this.minAttack = minAttack; }
    @Override
    public void execute(EffectContext context) {
      if (!(context.target instanceof Minion m)) return;
      if (m.currentAttack() < minAttack) return;
      m.takeDamage(Math.max(0, m.currentHealth()));
    }
  }

  public static class MultiplyHealthEffect implements ICardEffect {
    @Override
    public void execute(EffectContext context) {
      if (context.target instanceof Minion m) m.modifyHealth(m.currentHealth());
    }
  }

  public static class TransformEffect implements ICardEffect {
    @Override
    public void execute(EffectContext context) {
      if (!(context.target instanceof Minion target)) return;
      Player owner = target.ownerId.equals(context.player.id()) ? context.player : context.opponent;
      owner.replaceMinion(target.instanceId, new Minion(
          UUID.randomUUID().toString(),
          TokenCards.SHEEP_TOKEN.id(), TokenCards.SHEEP_TOKEN.name(),
          TokenCards.SHEEP_TOKEN.attack(), TokenCards.SHEEP_TOKEN.health(),
          owner.id(), false, TokenCards.SHEEP_TOKEN.imagePath()));
    }
  }
}
