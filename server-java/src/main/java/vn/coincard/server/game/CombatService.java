package vn.coincard.server.game;

import java.util.List;
import java.util.stream.Collectors;

/** Validates and resolves minion combat. */
public final class CombatService {
  private CombatService() {}

  public static void resolveAttack(Player player, Player opponent, String attackerId, String targetId) {
    Minion attacker = player.findMinion(attackerId);
    if (attacker == null) {
      throw new GameException.InvalidTarget("Attacker không tồn tại hoặc không phải quái của bạn.");
    }
    if (!attacker.getOwnerId().equals(player.id())) {
      throw new GameException.InvalidTarget("Không được dùng quái của đối thủ.");
    }
    if (!attacker.canAttack()) {
      throw new GameException.InvalidTarget("Minion đã hành động / summoning sickness.");
    }

    List<Minion> taunts = opponent.getBoard().stream()
        .filter(m -> m.hasTaunt() && !m.isDead()).collect(Collectors.toList());

    if (targetId.equals(opponent.id())) {
      if (!opponent.getBoard().isEmpty()) {
        throw new GameException.MinionsBlockHero();
      }
      player.recordDamage(opponent.heroState().takeDamage(attacker.currentAttack()));
      attacker.markAsAttacked();
      return;
    }
    if (targetId.equals(player.id())) {
      throw new GameException.InvalidTarget("Không được tấn công hero của chính mình.");
    }

    Minion defender = opponent.findMinion(targetId);
    if (defender == null) {
      throw new GameException.InvalidTarget("Chỉ được tấn công quái của đối thủ.");
    }
    if (!taunts.isEmpty() && !defender.hasTaunt()) {
      throw new GameException.TauntRequired();
    }
    opponent.recordDamage(attacker.takeDamage(defender.currentAttack()));
    player.recordDamage(defender.takeDamage(attacker.currentAttack()));
    attacker.markAsAttacked();
  }
}
