import { Player } from '../Player.js';
import type { ICardEffect } from './ICardEffect.js';
import type { EffectContext } from './EffectContext.js';

export class HealEffect implements ICardEffect {
  constructor(public readonly amount: number) {}

  execute(context: EffectContext): void {
    const target = context.target;
    if (target instanceof Player) {
      target.heroState.heal(this.amount);
    }
  }
}
