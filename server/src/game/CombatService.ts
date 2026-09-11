import type { Player } from './Player.js';
import { InvalidTargetError, TauntRequiredError } from './errors.js';

/**
 * Chiến đấu — minion của người đang đánh chỉ được đánh sang phe địch.
 * Validate toàn bộ trước, trừ lượt attacker sau cùng.
 */
export function resolveAttack(
  player: Player,
  opponent: Player,
  attackerId: string,
  targetId: string,
): void {
  const attacker = player.findMinion(attackerId);
  if (!attacker) {
    throw new InvalidTargetError('Attacker không tồn tại hoặc không phải quái của bạn.');
  }
  if (attacker.ownerId !== player.id) {
    throw new InvalidTargetError('Không được dùng quái của đối thủ.');
  }
  if (!attacker.canAttack) {
    throw new InvalidTargetError('Minion đã hành động / summoning sickness.');
  }

  const taunts = opponent.getBoard().filter((m) => m.hasTaunt && !m.isDead());

  if (targetId === opponent.id) {
    if (taunts.length > 0) {
      throw new TauntRequiredError();
    }
    player.recordDamage(opponent.heroState.takeDamage(attacker.currentAttack));
    attacker.markAsAttacked();
    return;
  }
  if (targetId === player.id) {
    throw new InvalidTargetError('Không được tấn công hero của chính mình.');
  }

  const defender = opponent.findMinion(targetId);
  if (!defender) {
    throw new InvalidTargetError('Chỉ được tấn công quái của đối thủ.');
  }
  if (taunts.length > 0 && !defender.hasTaunt) {
    throw new TauntRequiredError();
  }
  opponent.recordDamage(attacker.takeDamage(defender.currentAttack));
  player.recordDamage(defender.takeDamage(attacker.currentAttack));
  attacker.markAsAttacked();
}
