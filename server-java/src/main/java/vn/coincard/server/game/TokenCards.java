package vn.coincard.server.game;

import java.util.List;

/** Mirror of server/src/game/tokenCards.ts */
public final class TokenCards {
  private TokenCards() {}

  public static final CardTypes.CardDefinition SILVER_HAND_RECRUIT_TOKEN =
      new CardTypes.CardDefinition(
          "token-silver-hand", "Silver Hand Recruit", "silver-hand-recruit",
          "Hero Power token.", "MINION", "COMMON", 1, 1, 1, "PALADIN",
          "assets/images/Minions/Silver Hand Recruit.png",
          List.of(), List.of(), false);

  public static final CardTypes.CardDefinition SHEEP_TOKEN =
      new CardTypes.CardDefinition(
          "token-sheep", "Sheep", "sheep",
          "Polymorph token.", "MINION", "COMMON", 1, 1, 1, "MAGE",
          "assets/images/Minions/Sheep.png",
          List.of(), List.of(), false);
}
