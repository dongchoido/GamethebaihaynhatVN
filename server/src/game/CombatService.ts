import type { Player } from './Player.js';
import { InvalidTargetError } from './errors.js';

/**
 * Chiến đấu nội chung — minion→hero hoặc minion↔minion (đamheo băc).
 */
export function resolveAttack(
  player: Player,
  opponent: Player,
  attackerId: string,
  targetId: string,
): void {
  const attacker =
    player.findMinion(attackerId) ?? opponent.findMinion(attackerId);
  if (!attacker) {
    throw new InvalidTargetError('Attacker không tồn tại.');
  }
  if (!attacker.canAttack) {
    throw new InvalidTargetError('Minion đã hành động / summoning sickness.');
  }

  attacker.markAsAttacked();

  if (targetId === player.id || targetId === opponent.id) {
    const targetHero = targetId === opponent.id ? opponent : player;
    targetHero.heroState.takeDamage(attacker.currentAttack);
    return;
  }

  const defender = player.findMinion(targetId) ?? opponent.findMinion(targetId);
  if (!defender) {
    throw new InvalidTargetError('Target không tồn tại.');
  }
  attacker.takeDamage(defender.currentAttack);
  defender.takeDamage(attacker.currentAttack);
}
