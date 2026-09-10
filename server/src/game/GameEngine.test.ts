import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { GameEngine } from './GameEngine.js';
import { Hero } from './Hero.js';
import { Deck } from './Deck.js';
import { CardType, Rarity, HeroClass, type CardDefinition } from '@coincard/shared';
import { MAX_BOARD_SIZE } from './constants.js';

function makeMinionCard(overrides: Partial<CardDefinition> = {}): CardDefinition {
  return {
    id: `card-test-${Math.random().toString(36).slice(2)}`,
    name: 'Test Minion',
    slug: 'test-minion',
    description: 'Test.',
    type: CardType.MINION,
    rarity: Rarity.COMMON,
    manaCost: 1,
    attack: 2,
    health: 2,
    heroClass: HeroClass.NEUTRAL,
    imagePath: 'assets/images/Minions/Chilwind Yeti.png',
    effects: [],
    keywords: [],
    collectible: true,
    ...overrides,
  };
}

function makeHero(heroClass = 'MAGE'): Hero {
  return new Hero('hero_mage', 'Jaina', heroClass, 'Fireblast', 2, 'assets/images/Heros/Jaina Proudmoore.png');
}

function setupEngine() {
  const engine = new GameEngine();
  const deckCards = Array.from({ length: 30 }, () => makeMinionCard());
  const game = engine.createGame('game-1', 'ABC123', [
    { playerId: 'p1', name: 'One', hero: makeHero(), deck: new Deck(deckCards) },
    { playerId: 'p2', name: 'Two', hero: makeHero(), deck: new Deck(deckCards) },
  ]);
  game.start();
  return { engine, game };
}

describe('GameEngine', () => {
  it('chia bài mở đầu: p1 3 lá, p2 4 lá', () => {
    const { game } = setupEngine();
    assert.equal(game.getPlayerById('p1').handCount, 3);
    assert.equal(game.getPlayerById('p2').handCount, 4);
  });

  it('play minion trừ mana và lên board', () => {
    const { engine, game } = setupEngine();
    const player = game.getPlayerById('p1');
    const card = player.handCards[0];
    if (!card) throw new Error('No card');
    engine.playCard('game-1', 'p1', card.id);
    assert.equal(player.boardCount, 1);
    assert.equal(player.currentMana, 0);
  });

  it('chặn play khi không đủ mana', () => {
    const { engine, game } = setupEngine();
    const player = game.getPlayerById('p1');
    player.addToHand(makeMinionCard({ manaCost: 10 }));
    const expensive = player.handCards[player.handCards.length - 1];
    if (!expensive) throw new Error('No card');
    assert.throws(() => engine.playCard('game-1', 'p1', expensive.id));
  });

  it('chặn action ngoài turn', () => {
    const { engine, game } = setupEngine();
    const player = game.getPlayerById('p2');
    const card = player.handCards[0];
    if (!card) throw new Error('No card');
    assert.throws(() => engine.playCard('game-1', 'p2', card.id));
  });

  it('minion mới summon không tấn công ngay (summoning sickness)', () => {
    const { engine, game } = setupEngine();
    const player = game.getPlayerById('p1');
    const card = player.handCards[0];
    if (!card) throw new Error('No card');
    engine.playCard('game-1', 'p1', card.id);
    const minion = player.getBoard()[0];
    if (!minion) throw new Error('No minion');
    assert.throws(() => engine.attack('game-1', 'p1', minion.instanceId, 'p2'));
  });

  it('end turn đổi active player và tăng mana', () => {
    const { engine, game } = setupEngine();
    engine.endTurn('game-1', 'p1');
    assert.equal(game.getActivePlayerId(), 'p2');
    assert.equal(game.getPlayerById('p2').currentMaxMana, 1);
  });

  it('minion tấn hero gây damage và kết thúc game khi HP <= 0', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    const p2 = game.getPlayerById('p2');
    p2.heroState.takeDamage(29); // còn 1 HP
    const card = p1.handCards[0];
    if (!card) throw new Error('No card');
    engine.playCard('game-1', 'p1', card.id);
    const minion = p1.getBoard()[0];
    if (!minion) throw new Error('No minion');
    // Hết summoning sickness: end turn 2 lần để minion sẵn sàng
    engine.endTurn('game-1', 'p1');
    engine.endTurn('game-1', 'p2');
    engine.attack('game-1', 'p1', minion.instanceId, 'p2');
    assert.equal(game.isFinished(), true);
    assert.equal(game.getWinnerId(), 'p1');
  });

  it('minion vs minion cùng gây damage, con chết bị xóa', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    const p2 = game.getPlayerById('p2');
    // p1 summon
    const c1 = p1.handCards[0];
    if (!c1) throw new Error('No card');
    engine.playCard('game-1', 'p1', c1.id);
    engine.endTurn('game-1', 'p1');
    // Thêm 1/1 vào tay p2 sau khi draw đầu turn để chắc chắn lấy đúng lá
    p2.addToHand(makeMinionCard({ attack: 1, health: 1 }));
    // p2 summon con 1/1
    const c2 = p2.handCards[p2.handCards.length - 1];
    if (!c2) throw new Error('No card');
    assert.equal(c2.attack, 1);
    assert.equal(c2.health, 1);
    engine.playCard('game-1', 'p2', c2.id);
    engine.endTurn('game-1', 'p2');
    const attacker = p1.getBoard()[0];
    const defender = p2.getBoard()[0];
    if (!attacker || !defender) throw new Error('No minions');
    engine.attack('game-1', 'p1', attacker.instanceId, defender.instanceId);
    assert.equal(p2.boardCount, 0); // 1/1 chết
    assert.equal(p1.boardCount, 1); // 2/2 còn 1 HP
  });

  it(`board tối đa ${MAX_BOARD_SIZE} minion`, () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    for (let i = 0; i < MAX_BOARD_SIZE; i++) {
      p1.addToHand(makeMinionCard({ manaCost: 0 }));
    }
    p1.refillMana();
    // Cần mana: set maxMana cao bằng cách endTurn nhiều lần? Đơn giản: manaCost 0
    for (let i = 0; i < MAX_BOARD_SIZE; i++) {
      const card = p1.handCards[p1.handCards.length - 1];
      if (!card) throw new Error('No card');
      engine.playCard('game-1', 'p1', card.id);
    }
    assert.equal(p1.boardCount, MAX_BOARD_SIZE);
    p1.addToHand(makeMinionCard({ manaCost: 0 }));
    const extra = p1.handCards[p1.handCards.length - 1];
    if (!extra) throw new Error('No card');
    assert.throws(() => engine.playCard('game-1', 'p1', extra.id));
  });

  it('tay đầu luôn có lá rẻ (opening guarantee)', () => {
    for (let i = 0; i < 20; i++) {
      const { game } = setupEngine();
      const cheap1 = game.getPlayerById('p1').handCards.some((c) => c.manaCost <= 2);
      const cheap2 = game.getPlayerById('p2').handCards.some((c) => c.manaCost <= 2);
      assert.equal(cheap1, true);
      assert.equal(cheap2, true);
      assert.equal(game.getPlayerById('p1').handCount, 3);
      assert.equal(game.getPlayerById('p2').handCount, 4);
    }
  });
});
