export enum CardType {
  MINION = 'MINION',
  SPELL = 'SPELL',
  WEAPON = 'WEAPON',
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

export const EFFECT_TYPES = [
  'DAMAGE', 'AOE_DAMAGE', 'HEAL', 'DRAW', 'BUFF_ATTACK', 'BUFF_HEALTH',
  'MULTIPLY_HEALTH', 'SUMMON', 'TRANSFORM', 'DESTROY', 'DESTROY_ALL',
] as const;

export type EffectType = (typeof EFFECT_TYPES)[number];

export const CARD_KEYWORDS = ['TAUNT', 'CHARGE', 'DIVINE_SHIELD'] as const;
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

export interface HeroDefinition {
  id: string;
  name: string;
  heroClass: Exclude<HeroClass, HeroClass.NEUTRAL>;
  powerName: string;
  powerCost: number;
  imagePath: string;
}
