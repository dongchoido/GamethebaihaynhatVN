package vn.coincard.server.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Target-selection rule for a card effect. */
public enum EffectTarget {
  ENEMY_HERO,
  FRIENDLY_HERO,
  ENEMY_CHARACTER,
  FRIENDLY_CHARACTER,
  ANY_CHARACTER,
  ENEMY_MINION,
  FRIENDLY_MINION,
  ANY_MINION,
  SELF,
  RANDOM_ENEMY,
  ALL_MINIONS,
  ALL_ENEMY_MINIONS,
  ALL_FRIENDLY_MINIONS;

  @JsonCreator
  public static EffectTarget fromWire(String value) {
    return EnumParser.required(EffectTarget.class, value);
  }

  public static EffectTarget fromNullableWire(String value) {
    return value == null || value.isBlank() ? null : fromWire(value);
  }

  @JsonValue
  public String wireValue() {
    return name();
  }
}
