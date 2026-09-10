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
    context.opponent.getBoard().forEach((m) => m.takeDamage(this.amount));
  }
}

export class DestroyEffect implements ICardEffect {
  constructor(public readonly minAttack = 5) {}

  execute(context: EffectContext): void {
    const target = context.target as Minion | null;
    if (!target) {
      return;
    }
    if (target.currentAttack < this.minAttack) {
      return;
    }
    target.takeDamage(target.currentHealth);
  }
}

export class DestroyUntargetedEffect implements ICardEffect {
  execute(context: EffectContext): void {
    const target = context.target as Minion | null;
    if (!target) {
      return;
    }
    target.takeDamage(target.currentHealth);
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
    // Polymorph — giết target rồi summon Sheep 1/1 cho chủ sở hữu.
    target.takeDamage(target.currentHealth);
    const owner =
      target.ownerId === context.player.id ? context.player : context.opponent;
    owner.summonMinion(
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
