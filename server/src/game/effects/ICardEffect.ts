import type { EffectContext } from './EffectContext.js';

// Interface Segregation — class effect đi qua một interface duy nhất.
export interface ICardEffect {
  execute(context: EffectContext): void;
}

export function executeEffect(effect: ICardEffect, context: EffectContext): void {
  effect.execute(context);
}
