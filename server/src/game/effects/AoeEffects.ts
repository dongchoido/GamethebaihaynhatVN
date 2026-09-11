import type { ICardEffect } from './ICardEffect.js';
import type { EffectContext } from './EffectContext.js';
import type { Minion } from '../Minion.js';
import { Minion as MinionClass } from '../Minion.js';
import { makeUniqueCardId } from '../Deck.js';
import { SHEEP_TOKEN } from '../tokenCards.js';

// AOE / destroy / transform — dùng cho Flamestrike, Twisting Nether, Polymorph...
export class TwistingNetherEffect implements ICardEffect {
  execute(context: EffectContext): void {
    const all = [...context.player.getBoard(), ...context.opponent.getBoard()];
    all.forEach((m) => m.takeDamage(m.currentHealth));
  }
}

export class AoeDamageEffect implements ICardEffect {
  constructor(public readonly amount: number) {}

  execute(context: EffectContext): void {
    const targets = context.areaTargets ?? context.opponent.getBoard();
    targets.forEach(m => {
      const actual = m.takeDamage(this.amount);
      if (m.ownerId === context.opponent.id) context.player.recordDamage(actual);
    });
  }
}

export class DestroyEffect implements ICardEffect {
  constructor(public readonly minAttack = 0) {}

  execute(context: EffectContext): void {
    const target = context.target as Minion | null;
    if (!target) {
      return;
    }
    if (target.currentAttack < this.minAttack) {
      return;
    }
    target.takeDamage(Math.max(0, target.currentHealth));
  }
}

export class MultiplyHealthEffect implements ICardEffect {
  execute(context: EffectContext): void {
    const target = context.target as Minion | null;
    if (!target) {
      return;
    }
    // Divine Spirit — nhân đôi máu hiện tại.
    target.modifyHealth(target.currentHealth);
  }
}

export class TransformEffect implements ICardEffect {
  execute(context: EffectContext): void {
    const target = context.target as Minion | null;
    if (!target) {
      return;
    }
    // Thay tại chỗ: không cần slot trống, không tăng số lần summon.
    const owner =
      target.ownerId === context.player.id ? context.player : context.opponent;
    owner.replaceMinion(target.instanceId,
      new MinionClass(
        makeUniqueCardId(),
        SHEEP_TOKEN.id,
        SHEEP_TOKEN.name,
        SHEEP_TOKEN.attack,
        SHEEP_TOKEN.health,
        owner.id,
        false,
        SHEEP_TOKEN.imagePath,
      ),
    );
  }
}
