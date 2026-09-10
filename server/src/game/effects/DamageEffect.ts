import type { Minion } from '../Minion.js';
import { Player } from '../Player.js';
import type { ICardEffect } from './ICardEffect.js';
import type { EffectContext } from './EffectContext.js';

export class DamageEffect implements ICardEffect {
  constructor(public readonly amount: number) {}

  execute(context: EffectContext): void {
    const target = context.target;

    if (target instanceof Player) {
      target.heroState.takeDamage(this.amount);
      return;
    }

    if (target) {
      const minion = target as Minion;
      minion.takeDamage(this.amount);
      return;
    }
  }
}
