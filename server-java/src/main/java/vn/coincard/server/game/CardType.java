package vn.coincard.server.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Distinguishes summonable units from spells. */
public enum CardType {
  MINION,
  SPELL;

  @JsonCreator
  public static CardType fromWire(String value) {
    return EnumParser.required(CardType.class, value);
  }

  @JsonValue
  public String wireValue() {
    return name();
  }
}
