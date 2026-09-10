// Deck = hàng thẻ bài — Fisher-Yates shuffle (spec Randomness).
import { randomUUID } from 'node:crypto';
import type { CardDefinition } from '@coincard/shared';

export class Deck {
  private cards: CardDefinition[];

  constructor(cards: CardDefinition[]) {
    this.cards = cards.slice(); // copy — không để caller giữ lượt.
    this.shuffle();
  }

  get size(): number {
    return this.cards.length;
  }

  drawOne(): CardDefinition {
    const card = this.cards.shift();
    if (!card) {
      throw new Error('Deck is empty.');
    }
    return card;
  }

  drawMultiple(count: number): CardDefinition[] {
    return this.cards.splice(0, count);
  }

  isEmpty(): boolean {
    return this.cards.length === 0;
  }

  returnToBottom(card: CardDefinition): void {
    this.cards.push(card);
  }

  // Rút lá rẻ nhất trong deck (dùng cho opening-hand guarantee).
  drawCheapest(maxCost: number): CardDefinition | null {
    let cheapestIndex = -1;
    for (let i = 0; i < this.cards.length; i++) {
      const card = this.cards[i];
      if (!card || card.manaCost > maxCost) {
        continue;
      }
      const current = cheapestIndex === -1 ? null : this.cards[cheapestIndex];
      if (!current || card.manaCost < current.manaCost) {
        cheapestIndex = i;
      }
    }
    if (cheapestIndex === -1) {
      return null;
    }
    return this.cards.splice(cheapestIndex, 1)[0] ?? null;
  }

  /** Fisher-Yates shuffle — server chịu trách nhiệm (Randomness). */
  private shuffle(): void {
    for (let i = this.cards.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      const cardAtI = this.cards[i];
      const cardAtJ = this.cards[j];
      if (cardAtI === undefined || cardAtJ === undefined) {
        continue;
      }
      this.cards[i] = cardAtJ;
      this.cards[j] = cardAtI;
    }
  }
}

export function makeUniqueCardId(): string {
  return randomUUID();
}
