package vn.coincard.server.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Behavior implemented by an effect strategy. */
public enum EffectType {
  DAMAGE,
  HEAL,
  BUFF_ATTACK,
  BUFF_HEALTH,
  MULTIPLY_HEALTH,
  AOE_DAMAGE,
  TRANSFORM,
  DESTROY,
  DESTROY_ALL,
  TEMPORARY_MANA;

  @JsonCreator
  public static EffectType fromWire(String value) {
    return EnumParser.required(EffectType.class, value);
  }

  @JsonValue
  public String wireValue() {
    return name();
  }
}
