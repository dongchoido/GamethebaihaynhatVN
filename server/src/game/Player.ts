// Player: hero, deck, hand, board, mana — các method nắm trong object (Rule of thumb Ownership).
import { CardNotInHandError, BoardFullError } from './errors.js';
import { MAX_HAND_SIZE, MAX_BOARD_SIZE, MAX_MANA } from './constants.js';
import { Deck } from './Deck.js';
import { Hero } from './Hero.js';
import { Minion } from './Minion.js';
import type { CardDefinition } from '@coincard/shared';

export class Player {
  private mana: number;
  private maxMana: number;
  private hand: CardDefinition[];
  private readonly board: Minion[] = [];

  constructor(
    public readonly playerId: string,
    public readonly name: string,
    public readonly hero: Hero,
    private readonly deck: Deck,
  ) {
    this.mana = 0;
    this.maxMana = 0;
    this.hand = [];
  }

  get id(): string {
    return this.playerId;
  }

  get currentMana(): number {
    return this.mana;
  }

  get currentMaxMana(): number {
    return this.maxMana;
  }

  get handCards(): Readonly<CardDefinition[]> {
    return this.hand.map((c) => ({ ...c }));
  }

  get handCount(): number {
    return this.hand.length;
  }

  get boardCards(): Readonly<Minion[]> {
    return this.board;
  }

  get boardCount(): number {
    return this.board.length;
  }

  get deckSize(): number {
    return this.deck.size;
  }

  get heroState(): Hero {
    return this.hero;
  }

  hasEnoughMana(manaCost: number): boolean {
    return this.mana >= manaCost;
  }

  spendMana(amount: number): void {
    this.mana -= amount;
  }

  refillMana(): void {
    this.mana = this.maxMana;
  }

  increaseMaxMana(): void {
    this.maxMana = Math.min(this.maxMana + 1, MAX_MANA);
  }

  drawCard(): CardDefinition {
    return this.deck.drawOne();
  }

  drawFromHand(count: number): CardDefinition[] {
    return this.deck.drawMultiple(count);
  }

  drawAndAddToHand(count: number): void {
    for (let i = 0; i < count; i++) {
      try {
        this.addToHand(this.deck.drawOne());
      } catch {
        // Fatigue — deck hết thì phải phải chịu damage theo rule (ở đây không cần draw)
      }
    }
  }

  // Opening-hand guarantee: đảm bảo tay đầu có ít nhất 1 lá rẻ để luôn có nước đi.
  // Đổi lá đắt nhất tay lấy lá rẻ nhất deck (số lượng tay không đổi).
  guaranteeCheapOpener(maxCost: number): void {
    const hasCheap = this.hand.some((c) => c.manaCost <= maxCost);
    if (hasCheap) {
      return;
    }
    let expensiveIndex = -1;
    for (let i = 0; i < this.hand.length; i++) {
      const card = this.hand[i];
      const current = expensiveIndex === -1 ? null : this.hand[expensiveIndex];
      if (!card) {
        continue;
      }
      if (!current || card.manaCost > current.manaCost) {
        expensiveIndex = i;
      }
    }
    if (expensiveIndex === -1) {
      return;
    }
    const removed = this.hand.splice(expensiveIndex, 1)[0];
    if (!removed) {
      return;
    }
    this.deck.returnToBottom(removed);
    const cheap = this.deck.drawCheapest(maxCost);
    if (cheap) {
      this.addToHand(cheap);
    } else {
      this.addToHand(removed);
    }
  }

  /** Dùng bởi GameEngine/Mulligan ban đầu. */
  getBoard(): Minion[] {
    return this.board;
  }

  addToHand(card: CardDefinition): void {
    if (this.hand.length >= MAX_HAND_SIZE) {
      return; // HS rule: deck full → burn (đồng bộ UI)
    }
    this.hand.push(card);
  }

  removeFromHand(instanceId: string): CardDefinition {
    const index = this.hand.findIndex((c) => c.id === instanceId);
    if (index === -1) {
      throw new CardNotInHandError();
    }
    const removed = this.hand.splice(index, 1)[0];
    if (!removed) {
      throw new CardNotInHandError();
    }
    return removed;
  }

  summonMinion(minion: Minion): void {
    if (this.board.length >= MAX_BOARD_SIZE) {
      throw new BoardFullError();
    }
    this.board.push(minion);
  }

  removeMinion(instanceId: string): Minion | null {
    const index = this.board.findIndex((m) => m.instanceId === instanceId);
    if (index === -1) {
      return null;
    }
    return this.board.splice(index, 1)[0] ?? null;
  }

  findMinion(instanceId: string): Minion | null {
    return this.board.find((m) => m.instanceId === instanceId) ?? null;
  }

  removeDeadMinions(): void {
    for (let i = this.board.length - 1; i >= 0; i--) {
      const minion = this.board[i];
      if (minion !== undefined && minion.isDead()) {
        this.board.splice(i, 1);
      }
    }
  }
}
