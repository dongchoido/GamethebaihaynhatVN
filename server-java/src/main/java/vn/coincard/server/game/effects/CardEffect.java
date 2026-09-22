package vn.coincard.server.game.effects;

/** Strategy contract for one card effect. */
public interface CardEffect {
  void execute(EffectContext context);
}
