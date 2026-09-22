package vn.coincard.server.game;

import java.util.List;
import java.util.UUID;

/** Hero-power strategy registry. */
public interface HeroPower {
  HeroClass supportedClass();
  void validate(Player player);
  void execute(Game game, Player player);

  static List<HeroPower> defaults() {
    return List.of(
        new DamagePower(HeroClass.MAGE, 1),
        new DamagePower(HeroClass.HUNTER, 2),
        new RecruitPower(),
        new HealPower(),
        new DrawPower());
  }

  class DamagePower implements HeroPower {
    private final HeroClass heroClass;
    private final int amount;
    DamagePower(HeroClass heroClass, int amount) {
      this.heroClass = heroClass;
      this.amount = amount;
    }
    @Override public HeroClass supportedClass() { return heroClass; }
    @Override public void validate(Player player) {}
    @Override public void execute(Game game, Player player) {
      player.recordDamage(game.getOpponent().heroState().takeDamage(amount));
    }
  }

  class RecruitPower implements HeroPower {
    @Override public HeroClass supportedClass() { return HeroClass.PALADIN; }
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
    @Override public HeroClass supportedClass() { return HeroClass.PRIEST; }
    @Override public void validate(Player player) {}
    @Override public void execute(Game game, Player player) { player.heroState().heal(2); }
  }

  class DrawPower implements HeroPower {
    @Override public HeroClass supportedClass() { return HeroClass.WARLOCK; }
    @Override public void validate(Player player) {}
    @Override public void execute(Game game, Player player) {
      player.heroState().takeDamage(2);
      player.drawForTurn();
    }
  }
}
