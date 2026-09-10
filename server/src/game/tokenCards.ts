import { CardType, Rarity, HeroClass, type CardDefinition } from '@coincard/shared';

// Token sinh trong trận (hero power / Polymorph) — không có trong deck.
export const SILVER_HAND_RECRUIT_TOKEN: CardDefinition = {
  id: 'token_silver-hand-recruit',
  name: 'Silver Hand Recruit',
  slug: 'silver-hand-recruit',
  description: 'Hero Power token.',
  type: CardType.MINION,
  rarity: Rarity.COMMON,
  manaCost: 1,
  attack: 1,
  health: 1,
  heroClass: HeroClass.PALADIN,
  imagePath: 'assets/images/Minions/Silver Hand Recruit.png',
  effects: [],
  keywords: [],
  collectible: false,
};

export const SHEEP_TOKEN: CardDefinition = {
  id: 'token_sheep',
  name: 'Sheep',
  slug: 'sheep',
  description: 'Polymorph token.',
  type: CardType.MINION,
  rarity: Rarity.COMMON,
  manaCost: 1,
  attack: 1,
  health: 1,
  heroClass: HeroClass.MAGE,
  imagePath: 'assets/images/Minions/Sheep.png',
  effects: [],
  keywords: [],
  collectible: false,
};
