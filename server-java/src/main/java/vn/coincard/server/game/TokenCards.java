package vn.coincard.server.game;

import java.util.List;

/** Non-collectible cards created by effects and hero powers. */
public final class TokenCards {
  private TokenCards() {}

  public static final CardTypes.CardDefinition SILVER_HAND_RECRUIT_TOKEN =
      new CardTypes.CardDefinition(
          "token-silver-hand", "Silver Hand Recruit", "silver-hand-recruit",
          "Hero Power token.", CardType.MINION, Rarity.COMMON, 1, 1, 1, HeroClass.PALADIN,
          "assets/images/Minions/Silver Hand Recruit.png",
          List.of(), List.of(), false);

  public static final CardTypes.CardDefinition SHEEP_TOKEN =
      new CardTypes.CardDefinition(
          "token-sheep", "Sheep", "sheep",
          "Polymorph token.", CardType.MINION, Rarity.COMMON, 1, 1, 1, HeroClass.MAGE,
          "assets/images/Minions/Sheep.png",
      List.of(), List.of(), false);

  public static CardTypes.CardDefinition coinCard() {
    return new CardTypes.CardDefinition(
        "coin-token-" + java.util.UUID.randomUUID(), "The Coin", "the-coin",
        "Gain 1 temporary Mana Crystal this turn.", CardType.SPELL, Rarity.COMMON, 0, 0, 0,
        HeroClass.NEUTRAL, "assets/images/design/manacrystal.png",
        List.of(new CardTypes.EffectDefinition(
            EffectType.TEMPORARY_MANA, 1, EffectTarget.SELF, null, null, null)),
        List.of(), false);
  }
}
