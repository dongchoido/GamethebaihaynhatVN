package vn.coincard.server.game.effects;

import java.util.function.Function;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.EffectType;

/** Registry entry for a card-effect strategy. */
public interface EffectStrategy {
  EffectType supportedType();
  Function<CardTypes.EffectDefinition, CardEffect> create();
}
