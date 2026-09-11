import { effectNeedsTarget, type EffectDefinition } from '@coincard/shared';
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

// Effect cần target cụ thể — không có target hợp lệ thì reject, không cho cast.
// Dùng chung helper với client để hai bên không lệch nhau.
const TARGETED_EFFECT_TARGETS = {
  has: (target: EffectDefinition['target']): boolean => effectNeedsTarget(target),
};

/**
 * Chọn target + giải quyết effect.
 * Polymorphism: các effect implement `ICardEffect`.
 */
export class EffectResolver {
  private readonly strategies: Record<string, (effect: EffectDefinition) => ICardEffect> = {
    DAMAGE: e => new DamageEffect(e.value),
    HEAL: e => new HealEffect(e.value),
    BUFF_ATTACK: e => new BuffAttackEffect(e.value),
    BUFF_HEALTH: e => new BuffHealthEffect(e.value),
    MULTIPLY_HEALTH: () => new MultiplyHealthEffect(),
    AOE_DAMAGE: e => new AoeDamageEffect(e.value),
    TRANSFORM: () => new TransformEffect(),
    DESTROY: e => new DestroyEffect(e.minAttack ?? 0),
    DESTROY_ALL: () => new TwistingNetherEffect(),
  };
  /** Kiểm tra target hợp lệ mà không thay đổi state — validate trước khi commit. */
  validate(
    player: Player,
    opponent: Player,
    effect: EffectDefinition,
    targetId?: string,
  ): void {
    this.buildStrategy(effect);
    if (!Number.isFinite(effect.value) || effect.value < 0) throw new InvalidTargetError('Giá trị effect không hợp lệ.');
    const target = this.pickTarget(player, opponent, effect, targetId);
    if (['TRANSFORM', 'DESTROY', 'BUFF_ATTACK', 'BUFF_HEALTH', 'MULTIPLY_HEALTH'].includes(effect.type) && target instanceof Player) {
      throw new InvalidTargetError('Effect yêu cầu minion.');
    }
    if (effect.type === 'DESTROY' && target instanceof Minion && target.currentAttack < (effect.minAttack ?? 0)) {
      throw new InvalidTargetError('Công mục tiêu thấp hơn điều kiện của lá bài.');
    }
  }
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
    const areaTargets = effect.target === 'ALL_MINIONS' ? [...player.getBoard(), ...opponent.getBoard()]
      : effect.target === 'ALL_ENEMY_MINIONS' ? opponent.getBoard()
      : effect.target === 'ALL_FRIENDLY_MINIONS' ? player.getBoard() : undefined;
    executeEffect(strategy, { game, player, opponent, target, value: effect.value, areaTargets });
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
      // Effect cần target nhưng board rỗng → không cho cast (HS rule).
      if (TARGETED_EFFECT_TARGETS.has(effect.target)) {
        throw new InvalidTargetError('Không có mục tiêu hợp lệ.');
      }
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
  ): readonly (Minion | Player)[] {
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
      case 'SELF':
        return [player];
      case 'RANDOM_ENEMY':
        return []; // Server tự chọn ngẫu nhiên trong resolveRandomEnemies.
      case 'ALL_MINIONS':
      case 'ALL_ENEMY_MINIONS':
      case 'ALL_FRIENDLY_MINIONS':
        return []; // Effect không cần target — tự resolve trong strategy.
      default:
        // Fail-closed: target lạ (kể cả typo data) thì reject chứ không
        // mặc định cho đánh quái cả 2 bên.
        throw new InvalidTargetError(`Target chưa hỗ trợ: ${effect.target}.`);
    }
  }

  private buildStrategy(effect: EffectDefinition): ICardEffect {
    const factory = this.strategies[effect.type];
    if (!factory) throw new InvalidTargetError(`Effect chưa hỗ trợ: ${effect.type}.`);
    return factory(effect);
  }
}
