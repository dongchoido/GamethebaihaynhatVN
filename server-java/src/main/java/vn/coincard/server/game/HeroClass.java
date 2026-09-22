package vn.coincard.server.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Classes supported by the CoinCard catalog. */
public enum HeroClass {
  MAGE,
  HUNTER,
  PALADIN,
  PRIEST,
  WARLOCK,
  NEUTRAL;

  @JsonCreator
  public static HeroClass fromWire(String value) {
    return EnumParser.required(HeroClass.class, value);
  }

  @JsonValue
  public String wireValue() {
    return name();
  }

  public boolean canUse(CardTypes.CardDefinition card) {
    return card.heroClass() == NEUTRAL || card.heroClass() == this;
  }
}
