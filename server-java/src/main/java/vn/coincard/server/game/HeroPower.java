package vn.coincard.server.game;

import java.util.Map;
import java.util.UUID;

/** Mirror of HeroPower.ts: IHeroPower strategy + registry. */
public interface HeroPower {
  void validate(Player player);
  void execute(Game game, Player player);

  static HeroPower forClass(String heroClass) {
    HeroPower power = POWERS.get(heroClass);
    if (power == null) throw new GameException.InvalidTarget("Hero chưa được hỗ trợ.");
    return power;
  }

  Map<String, HeroPower> POWERS = Map.of(
      "MAGE", new DamagePower(1),
      "HUNTER", new DamagePower(2),
      "PALADIN", new RecruitPower(),
      "PRIEST", new HealPower(),
      "WARLOCK", new DrawPower());

  class DamagePower implements HeroPower {
    private final int amount;
    DamagePower(int amount) { this.amount = amount; }
    @Override public void validate(Player player) {}
    @Override public void execute(Game game, Player player) {
      player.recordDamage(game.getOpponent().heroState().takeDamage(amount));
    }
  }

  class RecruitPower implements HeroPower {
    @Override public void validate(Player player) {
      if (player.boardCount() >= Constants.MAX_BOARD_SIZE) throw new GameException.BoardFull();
    }
    @Override public void execute(Game game, Player player) {
      CardTypes.CardDefinition recruit = TokenCards.SILVER_HAND_RECRUIT_TOKEN;
      player.summonMinion(new Minion(UUID.randomUUID().toString(),
          recruit.id(), recruit.name(), recruit.attack(), recruit.health(),
          player.id(), false, recruit.imagePath()));
    }
  }

  class HealPower implements HeroPower {
    @Override public void validate(Player player) {}
    @Override public void execute(Game game, Player player) { player.heroState().heal(2); }
  }

  class DrawPower implements HeroPower {
    @Override public void validate(Player player) {}
    @Override public void execute(Game game, Player player) {
      player.heroState().takeDamage(2);
      if (player.deckSize() > 0) player.addToHand(player.drawCard());
    }
  }
}
