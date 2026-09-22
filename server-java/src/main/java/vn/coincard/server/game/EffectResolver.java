package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import vn.coincard.server.game.effects.BuffEffects;
import vn.coincard.server.game.effects.DamageEffect;
import vn.coincard.server.game.effects.HealEffect;
import vn.coincard.server.game.effects.AreaEffects;
import vn.coincard.server.game.effects.EffectContext;
import vn.coincard.server.game.effects.EffectStrategy;
import vn.coincard.server.game.effects.CardEffect;
import vn.coincard.server.game.effects.EffectFactory;

/** Card-effect strategy registry with fail-closed target validation. */
public class EffectResolver {
  private final RandomSource randomSource;

  private final EffectFactory effectFactory;

  public EffectResolver() {
    this(RandomSource.threadLocal());
  }

  public EffectResolver(RandomSource randomSource) {
    this(List.of(
        new Strategy(EffectType.DAMAGE, e -> new DamageEffect(e.value())),
        new Strategy(EffectType.HEAL, e -> new HealEffect(e.value())),
        new Strategy(EffectType.BUFF_ATTACK, e -> new BuffEffects.BuffAttackEffect(e.value())),
        new Strategy(EffectType.BUFF_HEALTH, e -> new BuffEffects.BuffHealthEffect(e.value())),
        new Strategy(EffectType.MULTIPLY_HEALTH, e -> new AreaEffects.MultiplyHealthEffect()),
        new Strategy(EffectType.AOE_DAMAGE, e -> new AreaEffects.AoeDamageEffect(e.value())),
        new Strategy(EffectType.TRANSFORM, e -> new AreaEffects.TransformEffect()),
        new Strategy(EffectType.DESTROY, e -> new AreaEffects.DestroyEffect(
            e.minAttack() != null ? e.minAttack() : 0)),
        new Strategy(EffectType.DESTROY_ALL, e -> new AreaEffects.TwistingNetherEffect()),
        new Strategy(EffectType.TEMPORARY_MANA,
            e -> context -> context.player.gainTemporaryMana(e.value()))),
        randomSource);
  }

  /** Constructor injection makes adding a strategy an extension, not a resolver edit. */
  public EffectResolver(List<EffectStrategy> strategies) {
    this(strategies, RandomSource.threadLocal());
  }

  public EffectResolver(List<EffectStrategy> strategies, RandomSource randomSource) {
    this.effectFactory = new EffectFactory(strategies);
    this.randomSource = randomSource;
  }

  private record Strategy(EffectType supportedType,
      Function<CardTypes.EffectDefinition, CardEffect> factory) implements EffectStrategy {
    @Override public Function<CardTypes.EffectDefinition, CardEffect> create() { return factory; }
  }

  public void validate(Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    buildStrategy(effect);
    if (effect.value() < 0) throw new GameException.InvalidTarget("Giá trị effect không hợp lệ.");
    var target = pickTarget(player, opponent, effect, targetId);
    if (effect.type() == EffectType.DESTROY && target instanceof Minion m
        && m.currentAttack() < (effect.minAttack() != null ? effect.minAttack() : 0)) {
      throw new GameException.InvalidTarget("Công mục tiêu thấp hơn điều kiện của lá bài.");
    }
  }

  public void resolve(Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    if (effect.target() == EffectTarget.RANDOM_ENEMY) {
      resolveRandomEnemies(player, opponent, effect);
      return;
    }
    var target = pickTarget(player, opponent, effect, targetId);
    List<Minion> areaTargets = null;
    if (effect.target() == EffectTarget.ALL_MINIONS) {
      areaTargets = new ArrayList<>(player.getBoard());
      areaTargets.addAll(opponent.getBoard());
    } else if (effect.target() == EffectTarget.ALL_ENEMY_MINIONS) {
      areaTargets = new ArrayList<>(opponent.getBoard());
    } else if (effect.target() == EffectTarget.ALL_FRIENDLY_MINIONS) {
      areaTargets = new ArrayList<>(player.getBoard());
    }
    buildStrategy(effect).execute(new EffectContext(player, opponent, target, areaTargets));
  }

  private void resolveRandomEnemies(Player player, Player opponent,
      CardTypes.EffectDefinition effect) {
    List<vn.coincard.server.model.GameCharacter> pool = new ArrayList<>(opponent.getBoard());
    pool.add(opponent.heroState());
    int pickCount = Math.min(effect.count() != null ? effect.count() : 2, pool.size());
    CardEffect strategy = buildStrategy(effect);
    for (int i = 0; i < pickCount; i++) {
      vn.coincard.server.model.GameCharacter picked = pool.remove(randomSource.nextInt(pool.size()));
      strategy.execute(new EffectContext(player, opponent, picked, null));
    }
  }

  private vn.coincard.server.model.GameCharacter pickTarget(Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    List<vn.coincard.server.model.GameCharacter> candidates =
        new ArrayList<>(validTargets(player, opponent, effect));
    if (targetId != null) {
      return candidates.stream().filter(t -> targetId.equals(characterId(t, player, opponent)))
          .findFirst()
          .orElseThrow(() -> new GameException.InvalidTarget("Target không thuộc danh sách hợp lệ."));
    }
    if (candidates.isEmpty()) {
      if (CardTypes.effectNeedsTarget(effect.target())) {
        throw new GameException.InvalidTarget("Không có mục tiêu hợp lệ.");
      }
      return null;
    }
    if (candidates.size() == 1) return candidates.get(0);
    throw new GameException.InvalidTarget("Cần chỉ định target.");
  }

  private List<vn.coincard.server.model.GameCharacter> validTargets(
      Player player, Player opponent, CardTypes.EffectDefinition effect) {
    List<vn.coincard.server.model.GameCharacter> out = new ArrayList<>();
    switch (effect.target()) {
      case ENEMY_HERO -> out.add(opponent.heroState());
      case FRIENDLY_HERO -> out.add(player.heroState());
      case ENEMY_CHARACTER -> { out.addAll(opponent.getBoard()); out.add(opponent.heroState()); }
      case FRIENDLY_CHARACTER -> { out.addAll(player.getBoard()); out.add(player.heroState()); }
      case ANY_CHARACTER -> {
        out.addAll(player.getBoard()); out.addAll(opponent.getBoard());
        out.add(player.heroState()); out.add(opponent.heroState());
      }
      case ENEMY_MINION -> out.addAll(opponent.getBoard());
      case FRIENDLY_MINION -> out.addAll(player.getBoard());
      case ANY_MINION -> { out.addAll(player.getBoard()); out.addAll(opponent.getBoard()); }
      case SELF -> out.add(player.heroState());
      case RANDOM_ENEMY, ALL_MINIONS, ALL_ENEMY_MINIONS, ALL_FRIENDLY_MINIONS -> { }
      default -> throw new GameException.InvalidTarget("Target chưa hỗ trợ: " + effect.target() + ".");
    }
    return out;
  }

  private CardEffect buildStrategy(CardTypes.EffectDefinition effect) {
    return effectFactory.create(effect);
  }

  private String characterId(vn.coincard.server.model.GameCharacter character, Player player,
      Player opponent) {
    if (character instanceof Minion minion) return minion.getInstanceId();
    if (character == player.heroState()) return player.id();
    if (character == opponent.heroState()) return opponent.id();
    return "";
  }

}
