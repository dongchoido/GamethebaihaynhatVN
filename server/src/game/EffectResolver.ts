import type { EffectDefinition } from '@coincard/shared';
import type { Game } from './Game.js';
import { Player } from './Player.js';
import { Minion } from './Minion.js';
import { DamageEffect } from './effects/DamageEffect.js';
import { HealEffect } from './effects/HealEffect.js';
import { BuffAttackEffect, BuffHealthEffect } from './effects/BuffEffects.js';
import {
  AoeDamageEffect,
  DestroyEffect,
  MultiplyHealthEffect,
  TransformEffect,
  TwistingNetherEffect,
} from './effects/AoeEffects.js';
import { executeEffect, type ICardEffect } from './effects/ICardEffect.js';
import { InvalidTargetError } from './errors.js';

/**
 * Chọn target + giải quyết effect.
 * Polymorphism: các effect implement `ICardEffect`.
 */
export class EffectResolver {
  resolve(
    game: Game,
    player: Player,
    opponent: Player,
    effect: EffectDefinition,
    targetId?: string,
  ): void {
    if (effect.target === 'RANDOM_ENEMY') {
      this.resolveRandomEnemies(game, player, opponent, effect);
      return;
    }
    const target = this.pickTarget(player, opponent, effect, targetId);
    const strategy = this.buildStrategy(effect);
    executeEffect(strategy, { game, player, opponent, target, value: effect.value });
  }

  private resolveRandomEnemies(
    game: Game,
    player: Player,
    opponent: Player,
    effect: EffectDefinition,
  ): void {
    // Multi-Shot — server chọn ngẫu nhiên, client không được quyết.
    const enemies: Array<Minion | Player> = [...opponent.getBoard(), opponent];
    const pickCount = Math.min(effect.count ?? 2, enemies.length);
    const pool = enemies.slice();
    for (let i = 0; i < pickCount; i++) {
      const pickIndex = Math.floor(Math.random() * pool.length);
      const picked = pool.splice(pickIndex, 1)[0];
      if (!picked) {
        continue;
      }
      executeEffect(this.buildStrategy(effect), {
        game,
        player,
        opponent,
        target: picked,
        value: effect.value,
      });
    }
  }

  private pickTarget(
    player: Player,
    opponent: Player,
    effect: EffectDefinition,
    targetId?: string,
  ): Minion | Player | null {
    const candidates = this.getValidTargets(player, opponent, effect);

    if (targetId) {
      const found = candidates.find(
        (t) =>
          (t instanceof Minion && t.instanceId === targetId) ||
          (t instanceof Player && t.id === targetId),
      );
      if (!found) {
        throw new InvalidTargetError('Target không thuộc danh sách hợp lệ.');
      }
      return found;
    }

    if (candidates.length === 0) {
      return null;
    }

    const single = candidates[0];
    if (candidates.length === 1 && single) {
      return single;
    }

    // Có nhiều target hợp lệ → client phải gửi targetId.
    throw new InvalidTargetError('Cần chỉ định target.');
  }

  private getValidTargets(
    player: Player,
    opponent: Player,
    effect: EffectDefinition,
  ): Array<Minion | Player> {
    switch (effect.target) {
      case 'ENEMY_HERO':
        return [opponent];
      case 'FRIENDLY_HERO':
        return [player];
      case 'ENEMY_CHARACTER':
        return [...opponent.getBoard(), opponent];
      case 'ANY_CHARACTER':
        return [...player.getBoard(), ...opponent.getBoard(), player, opponent];
      case 'ENEMY_MINION':
        return opponent.getBoard();
      case 'FRIENDLY_MINION':
        return player.getBoard();
      case 'ANY_MINION':
        return [...player.getBoard(), ...opponent.getBoard()];
      case 'ALL_MINIONS':
      case 'ALL_ENEMY_MINIONS':
      case 'ALL_FRIENDLY_MINIONS':
        return []; // Effect không cần target — tự resolve trong strategy.
      default:
        return [...player.getBoard(), ...opponent.getBoard()];
    }
  }

  private buildStrategy(effect: EffectDefinition): ICardEffect {
    switch (effect.type) {
      case 'DAMAGE':
        return new DamageEffect(effect.value);
      case 'HEAL':
        return new HealEffect(effect.value);
      case 'BUFF_ATTACK':
        return new BuffAttackEffect(effect.value);
      case 'BUFF_HEALTH':
        return new BuffHealthEffect(effect.value);
      case 'MULTIPLY_HEALTH':
        return new MultiplyHealthEffect();
      case 'AOE_DAMAGE':
        return new AoeDamageEffect(effect.value);
      case 'TRANSFORM':
        return new TransformEffect();
      case 'DESTROY':
        return new DestroyEffect(effect.minAttack ?? 5);
      case 'DESTROY_ALL':
        return new TwistingNetherEffect();
      default:
        throw new InvalidTargetError(`Effect chưa hỗ trợ: ${effect.type}.`);
    }
  }
}
