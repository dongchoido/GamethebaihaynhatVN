package vn.coincard.server.game;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Card/effect DTOs. Field names match the shared TypeScript types
 * (and the JSON stored in DB) exactly.
 */
public final class CardTypes {
  private CardTypes() {}

  public static final Set<String> EXPLICIT_TARGET_TYPES = Set.of(
      "ENEMY_MINION", "FRIENDLY_MINION", "ANY_MINION",
      "ENEMY_CHARACTER", "ANY_CHARACTER", "ENEMY_HERO");

  public static boolean effectNeedsTarget(String target) {
    return target != null && EXPLICIT_TARGET_TYPES.contains(target);
  }

  public record EffectDefinition(
      String type,
      int value,
      String target,
      Integer count,
      Integer minAttack,
      String cardSlug) {}

  public record CardDefinition(
      String id,
      String name,
      String slug,
      String description,
      String type,
      String rarity,
      int manaCost,
      int attack,
      int health,
      String heroClass,
      String imagePath,
      List<EffectDefinition> effects,
      List<String> keywords,
      boolean collectible) {}

  public record HeroDefinition(
      String id,
      String name,
      String heroClass,
      String powerName,
      int powerCost,
      String imagePath) {}

  @SuppressWarnings("unchecked")
  public static EffectDefinition effectFromMap(Map<String, Object> m) {
    return new EffectDefinition(
        (String) m.get("type"),
        m.get("value") == null ? 0 : ((Number) m.get("value")).intValue(),
        (String) m.get("target"),
        m.get("count") == null ? null : ((Number) m.get("count")).intValue(),
        m.get("minAttack") == null ? null : ((Number) m.get("minAttack")).intValue(),
        (String) m.get("cardSlug"));
  }

  @SuppressWarnings("unchecked")
  public static CardDefinition cardFromMap(Map<String, Object> m) {
    List<EffectDefinition> effects = ((List<Map<String, Object>>) m.getOrDefault("effects", List.of()))
        .stream().map(CardTypes::effectFromMap).toList();
    List<String> keywords = ((List<Object>) m.getOrDefault("keywords", List.of()))
        .stream().map(Object::toString).toList();
    return new CardDefinition(
        (String) m.get("id"),
        (String) m.getOrDefault("name", ""),
        (String) m.getOrDefault("slug", ""),
        (String) m.getOrDefault("description", ""),
        (String) m.getOrDefault("type", "MINION"),
        (String) m.getOrDefault("rarity", "COMMON"),
        m.get("manaCost") == null ? 0 : ((Number) m.get("manaCost")).intValue(),
        m.get("attack") == null ? 0 : ((Number) m.get("attack")).intValue(),
        m.get("health") == null ? 0 : ((Number) m.get("health")).intValue(),
        (String) m.getOrDefault("heroClass", "NEUTRAL"),
        (String) m.getOrDefault("imagePath", ""),
        effects, keywords,
        !Boolean.FALSE.equals(m.get("collectible")));
  }

  public static Map<String, Object> cardToState(CardDefinition c) {
    java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
    m.put("id", c.id());
    m.put("name", c.name());
    m.put("slug", c.slug());
    m.put("description", c.description());
    m.put("type", c.type());
    m.put("rarity", c.rarity());
    m.put("manaCost", c.manaCost());
    m.put("attack", c.attack());
    m.put("health", c.health());
    m.put("heroClass", c.heroClass());
    m.put("imagePath", c.imagePath());
    m.put("effects", c.effects().stream().map(CardTypes::effectToState).toList());
    m.put("keywords", c.keywords());
    m.put("collectible", c.collectible());
    return m;
  }

  private static Map<String, Object> effectToState(EffectDefinition e) {
    java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
    m.put("type", e.type());
    m.put("value", e.value());
    m.put("target", e.target());
    if (e.count() != null) m.put("count", e.count());
    if (e.minAttack() != null) m.put("minAttack", e.minAttack());
    if (e.cardSlug() != null) m.put("cardSlug", e.cardSlug());
    return m;
  }
}
