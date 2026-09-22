package vn.coincard.server.game.effects;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.EffectType;
import vn.coincard.server.game.GameException;

/** Creates card-effect strategies from the constructor-injected registry. */
public final class EffectFactory {
  private final Map<EffectType, Function<CardTypes.EffectDefinition, CardEffect>> factories;

  public EffectFactory(List<EffectStrategy> strategies) {
    factories = strategies.stream().collect(Collectors.toUnmodifiableMap(
        EffectStrategy::supportedType, EffectStrategy::create));
  }

  public CardEffect create(CardTypes.EffectDefinition effect) {
    Function<CardTypes.EffectDefinition, CardEffect> factory = factories.get(effect.type());
    if (factory == null) {
      throw new GameException.InvalidTarget("Effect chưa hỗ trợ: " + effect.type() + ".");
    }
    return factory.apply(effect);
  }
}
