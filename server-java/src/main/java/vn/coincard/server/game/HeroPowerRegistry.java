package vn.coincard.server.game;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Constructor-injected registry for hero-power strategies. */
public final class HeroPowerRegistry {
  private final Map<HeroClass, HeroPower> powers;

  public HeroPowerRegistry(List<HeroPower> strategies) {
    powers = strategies.stream().collect(Collectors.toUnmodifiableMap(
        HeroPower::supportedClass, Function.identity()));
  }

  public HeroPower forClass(HeroClass heroClass) {
    HeroPower power = powers.get(heroClass);
    if (power == null) throw new GameException.InvalidTarget("Hero chưa được hỗ trợ.");
    return power;
  }
}
