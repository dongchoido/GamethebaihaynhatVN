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

/** Mirror of EffectResolver.ts (strategy registry + fail-closed targets). */
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

  public void resolve(Game game, Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    if ("RANDOM_ENEMY".equals(effect.target())) {
      resolveRandomEnemies(game, player, opponent, effect);
      return;
    }
    Object target = pickTarget(game, player, opponent, effect, targetId);
    List<Minion> areaTargets = null;
    if ("ALL_MINIONS".equals(effect.target())) {
      areaTargets = new ArrayList<>(player.getBoard());
      areaTargets.addAll(opponent.getBoard());
    } else if ("ALL_ENEMY_MINIONS".equals(effect.target())) {
      areaTargets = new ArrayList<>(opponent.getBoard());
    } else if ("ALL_FRIENDLY_MINIONS".equals(effect.target())) {
      areaTargets = new ArrayList<>(player.getBoard());
    }
    buildStrategy(effect).execute(new EffectContext(game, player, opponent, target, effect.value(), areaTargets));
  }

  private void resolveRandomEnemies(Game game, Player player, Player opponent,
      CardTypes.EffectDefinition effect) {
    List<Object> pool = new ArrayList<>(opponent.getBoard());
    pool.add(opponent);
    int pickCount = Math.min(effect.count() != null ? effect.count() : 2, pool.size());
    ICardEffect strategy = buildStrategy(effect);
    for (int i = 0; i < pickCount; i++) {
      Object picked = pool.remove(random.nextInt(pool.size()));
      strategy.execute(new EffectContext(game, player, opponent, picked, effect.value(), null));
    }
  }

  private Object pickTarget(Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    return pickTarget(null, player, opponent, effect, targetId);
  }

  private Object pickTarget(Game game, Player player, Player opponent,
      CardTypes.EffectDefinition effect, String targetId) {
    List<Object> candidates = new ArrayList<>(validTargets(player, opponent, effect));
    if (targetId != null) {
      return candidates.stream().filter(t ->
          (t instanceof Minion m && m.instanceId.equals(targetId))
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

  /** State helpers kept here so network layer serializes identically to TS. */
  public static Map<String, Object> minionToState(Minion m) {
    Map<String, Object> m2 = new LinkedHashMap<>();
    m2.put("instanceId", m.instanceId);
    m2.put("cardId", m.cardId);
    m2.put("name", m.name);
    m2.put("attack", m.currentAttack());
    m2.put("health", m.currentHealth());
    m2.put("maxHealth", m.maxHealth());
    m2.put("canAttack", m.canAttack());
    m2.put("hasTaunt", m.hasTaunt);
    m2.put("imagePath", m.imagePath);
    return m2;
  }

  public static Map<String, Object> heroToState(Hero h) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("heroId", h.heroId);
    m.put("name", h.name);
    m.put("heroClass", h.heroClass);
    m.put("health", h.currentHealth());
    m.put("maxHealth", h.maxHealth());
    m.put("imagePath", h.imagePath);
    m.put("powerName", h.powerName);
    m.put("powerCost", h.powerCost);
    return m;
  }
}
