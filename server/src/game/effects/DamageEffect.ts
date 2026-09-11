import type { Minion } from '../Minion.js';
import { Player } from '../Player.js';
import type { ICardEffect } from './ICardEffect.js';
import type { EffectContext } from './EffectContext.js';

export class DamageEffect implements ICardEffect {
  constructor(public readonly amount: number) {}

  execute(context: EffectContext): void {
    const target = context.target;

    if (target instanceof Player) {
      const actual = target.heroState.takeDamage(this.amount);
      if (target.id === context.opponent.id) {
        context.player.recordDamage(actual);
      }
      return;
    }

    if (target) {
      const minion = target as Minion;
      const actual = minion.takeDamage(this.amount);
      if (minion.ownerId === context.opponent.id) {
        context.player.recordDamage(actual);
      }
      return;
    }
  }
}
