package vn.coincard.server.game;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Card/effect DTOs. Field names match the shared TypeScript types
 * (and the JSON stored in DB) exactly.
 */
public final class CardTypes {
  private CardTypes() {}

  public static final Set<EffectTarget> EXPLICIT_TARGET_TYPES = Set.of(
      EffectTarget.ENEMY_MINION, EffectTarget.FRIENDLY_MINION, EffectTarget.ANY_MINION,
      EffectTarget.ENEMY_CHARACTER, EffectTarget.FRIENDLY_CHARACTER,
      EffectTarget.ANY_CHARACTER);

  public static boolean effectNeedsTarget(EffectTarget target) {
    return target != null && EXPLICIT_TARGET_TYPES.contains(target);
  }

  public record EffectDefinition(
      EffectType type,
      int value,
      EffectTarget target,
      Integer count,
      Integer minAttack,
      String cardSlug) {
    public EffectDefinition {
      Objects.requireNonNull(type, "type");
    }
  }

  public record CardDefinition(
      String id,
      String name,
      String slug,
      String description,
      CardType type,
      Rarity rarity,
      int manaCost,
      int attack,
      int health,
      HeroClass heroClass,
      String imagePath,
      List<EffectDefinition> effects,
      List<Keyword> keywords,
      boolean collectible) {
    public CardDefinition {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(slug, "slug");
      Objects.requireNonNull(description, "description");
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(rarity, "rarity");
      Objects.requireNonNull(heroClass, "heroClass");
      Objects.requireNonNull(imagePath, "imagePath");
      effects = effects == null ? List.of() : List.copyOf(effects);
      keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
  }

}
