export enum CardType {
  MINION = 'MINION',
  SPELL = 'SPELL',
}

export enum Rarity {
  COMMON = 'COMMON',
  RARE = 'RARE',
  EPIC = 'EPIC',
  LEGENDARY = 'LEGENDARY',
}

export enum HeroClass {
  MAGE = 'MAGE',
  HUNTER = 'HUNTER',
  PALADIN = 'PALADIN',
  PRIEST = 'PRIEST',
  WARLOCK = 'WARLOCK',
  NEUTRAL = 'NEUTRAL',
}

export const EFFECT_TARGETS = [
  'FRIENDLY_MINION', 'ENEMY_MINION', 'ANY_MINION',
  'ENEMY_HERO', 'FRIENDLY_HERO', 'ENEMY_CHARACTER', 'ANY_CHARACTER',
  'ALL_ENEMY_MINIONS', 'ALL_FRIENDLY_MINIONS', 'ALL_MINIONS', 'RANDOM_ENEMY', 'SELF',
] as const;

export type EffectTarget = (typeof EFFECT_TARGETS)[number];

export const EXPLICIT_TARGET_TYPES: ReadonlySet<EffectTarget> = new Set([
  'ENEMY_MINION',
  'FRIENDLY_MINION',
  'ANY_MINION',
  'ENEMY_CHARACTER',
  'ANY_CHARACTER',
  'ENEMY_HERO',
]);

export function effectNeedsTarget(target: EffectTarget): boolean {
  return EXPLICIT_TARGET_TYPES.has(target);
}

export const EFFECT_TYPES = [
  'DAMAGE', 'AOE_DAMAGE', 'HEAL', 'BUFF_ATTACK', 'BUFF_HEALTH',
  'MULTIPLY_HEALTH', 'TRANSFORM', 'DESTROY', 'DESTROY_ALL',
] as const;

export type EffectType = (typeof EFFECT_TYPES)[number];

export const CARD_KEYWORDS = ['TAUNT', 'CHARGE'] as const;
export type CardKeyword = (typeof CARD_KEYWORDS)[number];

export interface EffectDefinition {
  type: EffectType;
  value: number;
  target: EffectTarget;
  count?: number;
  minAttack?: number;
  cardSlug?: string;
}

export interface CardDefinition {
  id: string;
  name: string;
  slug: string;
  description: string;
  type: CardType;
  rarity: Rarity;
  manaCost: number;
  attack: number;
  health: number;
  heroClass: HeroClass;
  imagePath: string;
  effects: EffectDefinition[];
  keywords: CardKeyword[];
  collectible: boolean;
}
