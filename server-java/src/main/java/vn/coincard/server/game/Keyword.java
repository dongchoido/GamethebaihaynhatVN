package vn.coincard.server.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Persistent card traits understood by the domain rules. */
public enum Keyword {
  CHARGE,
  TAUNT;

  @JsonCreator
  public static Keyword fromWire(String value) {
    return EnumParser.required(Keyword.class, value);
  }

  @JsonValue
  public String wireValue() {
    return name();
  }
}
