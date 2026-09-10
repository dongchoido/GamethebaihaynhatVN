import type { ICardEffect } from './ICardEffect.js';
import type { EffectContext } from './EffectContext.js';
import type { Minion } from '../Minion.js';

export class BuffAttackEffect implements ICardEffect {
  constructor(public readonly amount: number) {}

  execute(context: EffectContext): void {
    const target = context.target as Minion | null;
    if (!target) {
      // Không có target → buff toàn bộ friendly minions.
      context.player.getBoard().forEach((m) => m.modifyAttack(this.amount));
      return;
    }
    target.modifyAttack(this.amount);
  }
}

export class BuffHealthEffect implements ICardEffect {
  constructor(public readonly amount: number) {}

  execute(context: EffectContext): void {
    const target = context.target as Minion | null;
    if (!target) {
      context.player.getBoard().forEach((m) => m.modifyHealth(this.amount));
      return;
    }
    target.modifyHealth(this.amount);
  }
}
