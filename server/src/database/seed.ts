import { PrismaClient } from '@prisma/client';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

type CardSeed = {
  name: string;
  slug: string;
  type: string;
  rarity: string;
  manaCost: number;
  attack: number;
  health: number;
  heroClass: string;
  imagePath: string;
  description: string;
  effects: Array<{ type: string; value: number; target: string }>;
  keywords?: string[];
};

const prisma = new PrismaClient();
const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');

const heroes = [
  ['mage', 'Jaina Proudmoore', 'MAGE', 'Fireblast', 2, 'assets/images/Heros/Jaina Proudmoore.png'],
  ['hunter', 'Rexxar', 'HUNTER', 'Steady Shot', 2, 'assets/images/Heros/Rexxar.png'],
  ['paladin', 'Uther Lightbringer', 'PALADIN', 'Reinforce', 2, 'assets/images/Heros/Uther Lightbringer.png'],
  ['priest', 'Anduin Wrynn', 'PRIEST', 'Lesser Heal', 2, 'assets/images/Heros/Anduin-Wrynn.png'],
  ['warlock', "Gul'dan", 'WARLOCK', 'Life Tap', 2, "assets/images/Heros/Gul'dan.png"],
] as const;

async function main() {
  const cardsPath = path.join(rootDir, 'data', 'cards.json');
  const cards = JSON.parse(await readFile(cardsPath, 'utf8')) as CardSeed[];

  for (const card of cards) {
    const { keywords, ...rest } = card;
    await prisma.card.upsert({
      where: { slug: card.slug },
      update: { ...rest, keywords: keywords ?? [] },
      create: { id: `card_${card.slug}`, ...rest, keywords: keywords ?? [] },
    });
  }

  for (const [id, name, heroClass, powerName, powerCost, imagePath] of heroes) {
    await prisma.hero.upsert({
      where: { heroClass },
      update: { name, powerName, powerCost, imagePath },
      create: { id: `hero_${id}`, name, heroClass, powerName, powerCost, imagePath },
    });
  }

  console.log(`Seeded ${cards.length} cards and ${heroes.length} heroes.`);
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => prisma.$disconnect());
