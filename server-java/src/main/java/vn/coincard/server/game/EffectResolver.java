package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import vn.coincard.server.game.effects.BuffEffects;
import vn.coincard.server.game.effects.DamageEffect;
import vn.coincard.server.game.effects.HealEffect;
import vn.coincard.server.game.effects.AreaEffects;
import vn.coincard.server.game.effects.EffectContext;
import vn.coincard.server.game.effects.ICardEffect;

/** Card-effect strategy registry with fail-closed target validation. */
public class EffectResolver {
  private final Random random = new Random();

  private final Map<String, Function<CardTypes.EffectDefinition, ICardEffect>> strategies = Map.of(
      "DAMAGE", e -> new DamageEffect(e.value()),
      "HEAL", e -> new HealEffect(e.value()),
      "BUFF_ATTACK", e -> new BuffEffects.BuffAttackEffect(e.value()),
      "BUFF_HEALTH", e -> new BuffEffects.BuffHealthEffect(e.value()),
      "MULTIPLY_HEALTH", e -> new AreaEffects.MultiplyHealthEffect(),
      "AOE_DAMAGE", e -> new AreaEffects.AoeDamageEffect(e.value()),
      "TRANSFORM", e -> new AreaEffects.TransformEffect(),
      "DESTROY", e -> new AreaEffects.DestroyEffect(e.minAttack() != null ? e.minAttack() : 0),
      "DESTROY_ALL", e -> new AreaEffects.TwistingNetherEffect());

  public void validate(Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    buildStrategy(effect);
    if (effect.value() < 0) throw new GameException.InvalidTarget("Giá trị effect không hợp lệ.");
    Object target = pickTarget(player, opponent, effect, targetId);
    if (List.of("TRANSFORM", "DESTROY", "BUFF_ATTACK", "BUFF_HEALTH", "MULTIPLY_HEALTH")
        .contains(effect.type()) && target instanceof Player) {
      throw new GameException.InvalidTarget("Effect yêu cầu minion.");
    }
    if ("DESTROY".equals(effect.type()) && target instanceof Minion m
        && m.currentAttack() < (effect.minAttack() != null ? effect.minAttack() : 0)) {
      throw new GameException.InvalidTarget("Công mục tiêu thấp hơn điều kiện của lá bài.");
    }
  }

  public void resolve(Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    if ("RANDOM_ENEMY".equals(effect.target())) {
      resolveRandomEnemies(player, opponent, effect);
      return;
    }
    Object target = pickTarget(player, opponent, effect, targetId);
    List<Minion> areaTargets = null;
    if ("ALL_MINIONS".equals(effect.target())) {
      areaTargets = new ArrayList<>(player.getBoard());
      areaTargets.addAll(opponent.getBoard());
    } else if ("ALL_ENEMY_MINIONS".equals(effect.target())) {
      areaTargets = new ArrayList<>(opponent.getBoard());
    } else if ("ALL_FRIENDLY_MINIONS".equals(effect.target())) {
      areaTargets = new ArrayList<>(player.getBoard());
    }
    buildStrategy(effect).execute(new EffectContext(player, opponent, target, areaTargets));
  }

  private void resolveRandomEnemies(Player player, Player opponent,
      CardTypes.EffectDefinition effect) {
    List<Object> pool = new ArrayList<>(opponent.getBoard());
    pool.add(opponent);
    int pickCount = Math.min(effect.count() != null ? effect.count() : 2, pool.size());
    ICardEffect strategy = buildStrategy(effect);
    for (int i = 0; i < pickCount; i++) {
      Object picked = pool.remove(random.nextInt(pool.size()));
      strategy.execute(new EffectContext(player, opponent, picked, null));
    }
  }

  private Object pickTarget(Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    List<Object> candidates = new ArrayList<>(validTargets(player, opponent, effect));
    if (targetId != null) {
      return candidates.stream().filter(t ->
          (t instanceof Minion m && m.getInstanceId().equals(targetId))
              || (t instanceof Player p && p.id().equals(targetId)))
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

  private List<Object> validTargets(Player player, Player opponent, CardTypes.EffectDefinition effect) {
    List<Object> out = new ArrayList<>();
    switch (effect.target()) {
      case "ENEMY_HERO" -> out.add(opponent);
      case "FRIENDLY_HERO" -> out.add(player);
      case "ENEMY_CHARACTER" -> { out.addAll(opponent.getBoard()); out.add(opponent); }
      case "ANY_CHARACTER" -> {
        out.addAll(player.getBoard()); out.addAll(opponent.getBoard());
        out.add(player); out.add(opponent);
      }
      case "ENEMY_MINION" -> out.addAll(opponent.getBoard());
      case "FRIENDLY_MINION" -> out.addAll(player.getBoard());
      case "ANY_MINION" -> { out.addAll(player.getBoard()); out.addAll(opponent.getBoard()); }
      case "SELF" -> out.add(player);
      case "RANDOM_ENEMY", "ALL_MINIONS", "ALL_ENEMY_MINIONS", "ALL_FRIENDLY_MINIONS" -> { }
      default -> throw new GameException.InvalidTarget("Target chưa hỗ trợ: " + effect.target() + ".");
    }
    return out;
  }

  private ICardEffect buildStrategy(CardTypes.EffectDefinition effect) {
    Function<CardTypes.EffectDefinition, ICardEffect> factory = strategies.get(effect.type());
    if (factory == null) throw new GameException.InvalidTarget("Effect chưa hỗ trợ: " + effect.type() + ".");
    return factory.apply(effect);
  }

  /** Delegates to mapper — giữ API cũ cho tương thích. */
  public static Map<String, Object> minionToState(Minion m) {
    return vn.coincard.server.mapper.GameStateMapper.toMinionState(m);
  }

  public static Map<String, Object> heroToState(Hero h) {
    return vn.coincard.server.mapper.GameStateMapper.toHeroState(h);
  }
}
