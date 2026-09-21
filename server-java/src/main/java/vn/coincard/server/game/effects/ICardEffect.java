package vn.coincard.server.game.effects;

/** Strategy contract for one card effect. */
public interface ICardEffect {
  void execute(EffectContext context);
}
