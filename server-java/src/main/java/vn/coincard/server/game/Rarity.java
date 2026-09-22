package vn.coincard.server.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Copy-limit category for a card. */
public enum Rarity {
  COMMON,
  RARE,
  EPIC,
  LEGENDARY;

  @JsonCreator
  public static Rarity fromWire(String value) {
    return EnumParser.required(Rarity.class, value);
  }

  @JsonValue
  public String wireValue() {
    return name();
  }

  public int copyLimit() {
    return this == LEGENDARY ? 1 : 2;
  }
}
