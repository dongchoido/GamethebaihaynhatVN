// Nơi DUY NHẤT biết đường dẫn ảnh — UI không hard-code path.
// Ảnh server trả về dạng 'assets/...' (relative) → resolve thành URL public + encode.

const PUBLIC_BASE = '/';

export function resolveAsset(relativePath: string): string {
  const normalized = relativePath.startsWith('/') ? relativePath.slice(1) : relativePath;
  return encodeURI(`${PUBLIC_BASE}${normalized}`);
}

export interface HeroAsset {
  heroClass: string;
  name: string;
  portrait: string;
  power: string;
  powerDisabled: string;
  powerCost: number;
  winSplash: string;
  defeatSplash: string;
}

const HERO_ASSETS: Record<string, HeroAsset> = {
  MAGE: {
    heroClass: 'MAGE',
    name: 'Jaina',
    portrait: 'assets/images/Heros/Jaina Proudmoore.png',
    power: 'assets/images/HeroPower/Mage.png',
    powerDisabled: 'assets/images/HeroPower/Mage Disabled.png',
    powerCost: 2,
    winSplash: 'assets/images/Heros/Mage-Win.png',
    defeatSplash: 'assets/images/Heros/Mage-Defeat.png',
  },
  HUNTER: {
    heroClass: 'HUNTER',
    name: 'Rexxar',
    portrait: 'assets/images/Heros/Rexxar.png',
    power: 'assets/images/HeroPower/Hunter.png',
    powerDisabled: 'assets/images/HeroPower/Hunter Disabled.png',
    powerCost: 2,
    winSplash: 'assets/images/Heros/Hunter-Win.png',
    defeatSplash: 'assets/images/Heros/Hunter-Defeat.png',
  },
  PALADIN: {
    heroClass: 'PALADIN',
    name: 'Uther',
    portrait: 'assets/images/Heros/Uther Lightbringer.png',
    power: 'assets/images/HeroPower/Paladin.png',
    powerDisabled: 'assets/images/HeroPower/Paladin Disabled.png',
    powerCost: 2,
    winSplash: 'assets/images/Heros/Paladin-Win.png',
    defeatSplash: 'assets/images/Heros/Paladin-Defeat.png',
  },
  PRIEST: {
    heroClass: 'PRIEST',
    name: 'Anduin',
    portrait: 'assets/images/Heros/Anduin-Wrynn.png',
    power: 'assets/images/HeroPower/Priest.png',
    powerDisabled: 'assets/images/HeroPower/Priest Disabled.png',
    powerCost: 2,
    winSplash: 'assets/images/Heros/Priest-Win.png',
    defeatSplash: 'assets/images/Heros/Priest-Defeat.png',
  },
  WARLOCK: {
    heroClass: 'WARLOCK',
    name: "Gul'dan",
    portrait: "assets/images/Heros/Gul'dan.png",
    power: 'assets/images/HeroPower/Warlock.png',
    powerDisabled: 'assets/images/HeroPower/Warlock Disabled.png',
    powerCost: 2,
    winSplash: 'assets/images/Heros/Warlock-Win.png',
    defeatSplash: 'assets/images/Heros/Warlock-Defeat.png',
  },
};

export function getHeroAsset(heroClass: string): HeroAsset {
  return (
    HERO_ASSETS[heroClass] ?? {
      heroClass,
      name: heroClass,
      portrait: 'assets/images/design/NoCardView.png',
      power: 'assets/images/HeroPower/Mage.png',
      powerDisabled: 'assets/images/HeroPower/Mage Disabled.png',
      powerCost: 2,
      winSplash: 'assets/images/Heros/Mage-Win.png',
      defeatSplash: 'assets/images/Heros/Mage-Defeat.png',
    }
  );
}

export function listHeroAssets(): HeroAsset[] {
  return Object.values(HERO_ASSETS);
}

export const UI_IMAGE = {
  playground: 'assets/images/design/ArenaWood.jpg',
  surrender: 'assets/images/design/SurrenderButton.png',
  startBackground: 'assets/images/design/StartBG.jpg',
  shopBackground: 'assets/images/design/ShopBackground.jpg',
  cardBack: 'assets/images/design/CardViewBack.png',
  cardBackBurned: 'assets/images/design/BurnedBack.jpg',
  noCard: 'assets/images/design/NoCardView.png',
  manaCrystal: 'assets/images/design/manacrystal.png',
  manaCrystalDark: 'assets/images/design/manacrystal_dark.png',
  healthBadge: 'assets/images/design/HealthBG.png',
  endTurn: 'assets/images/design/EndTurnButton.png',
} as const;

export const SOUND = {
  attack: 'assets/sounds/Attack.wav',
  endTurn: 'assets/sounds/EndTurn.wav',
  play: 'assets/sounds/Play.wav',
  playCard: 'assets/sounds/PlayCard.wav',
  heroSelect: 'assets/sounds/HeroSelect.wav',
  start: 'assets/sounds/Start.wav',
  victory: 'assets/sounds/Victory.wav',
  intro: 'assets/sounds/Intro.wav',
} as const;

export function playSound(relativePath: string): void {
  try {
    const audio = new Audio(resolveAsset(relativePath));
    void audio.play().catch(() => undefined);
  } catch {
    // Âm thanh fail không được làm sập game.
  }
}
