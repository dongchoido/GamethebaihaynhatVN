import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { GameEngine } from './GameEngine.js';
import { Hero } from './Hero.js';
import { Deck } from './Deck.js';
import { Minion } from './Minion.js';
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

function makeSpellCard(overrides: Partial<CardDefinition> = {}): CardDefinition {
  return {
    ...makeMinionCard(),
    id: `spell-test-${Math.random().toString(36).slice(2)}`,
    name: 'Target Spell',
    slug: 'target-spell',
    type: CardType.SPELL,
    manaCost: 0,
    attack: 0,
    health: 0,
    effects: [{ type: 'DAMAGE', value: 2, target: 'ANY_MINION' }],
    ...overrides,
  };
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
    // Tay giới hạn 6 lá: xả tay mở đầu, đánh theo đợt để đủ board 7.
    for (const c of [...p1.handCards]) {
      p1.removeFromHand(c.id);
    }
    p1.refillMana();
    let played = 0;
    while (played < MAX_BOARD_SIZE) {
      const need = Math.min(6 - p1.handCount, MAX_BOARD_SIZE - played);
      for (let i = 0; i < need; i++) {
        p1.addToHand(makeMinionCard({ manaCost: 0 }));
      }
      const batch = p1.handCount;
      for (let i = 0; i < batch && played < MAX_BOARD_SIZE; i++) {
        const card = p1.handCards[p1.handCards.length - 1];
        if (!card) throw new Error('No card');
        engine.playCard('game-1', 'p1', card.id);
        played++;
      }
    }
    assert.equal(p1.boardCount, MAX_BOARD_SIZE);
    p1.addToHand(makeMinionCard({ manaCost: 0 }));
    const extra = p1.handCards[p1.handCards.length - 1];
    if (!extra) throw new Error('No card');
    assert.throws(() => engine.playCard('game-1', 'p1', extra.id));
  });

  it('board đầy: action bị reject không tăng mana/maxMana và không mất bài', () => {
    const { engine, game } = setupEngine();
    const player = game.getPlayerById('p1');
    for (const c of [...player.handCards]) {
      player.removeFromHand(c.id);
    }
    for (let i = 0; i < MAX_BOARD_SIZE; i++) {
      const card = makeMinionCard({ manaCost: 0 });
      player.addToHand(card);
      engine.playCard('game-1', 'p1', card.id);
    }
    const rejected = makeMinionCard({ manaCost: 0 });
    player.addToHand(rejected);
    const manaBefore = player.currentMana;
    const maxManaBefore = player.currentMaxMana;
    const handBefore = player.handCount;
    assert.throws(() => engine.playCard('game-1', 'p1', rejected.id));
    assert.equal(player.currentMana, manaBefore);
    assert.equal(player.currentMaxMana, maxManaBefore);
    assert.equal(player.handCount, handBefore);
    assert.equal(player.findCardInHand(rejected.id)?.id, rejected.id);
  });

  it('spell không có target: reject nguyên tử, không mất mana/bài', () => {
    const { engine, game } = setupEngine();
    const player = game.getPlayerById('p1');
    const spell = makeSpellCard();
    player.addToHand(spell);
    const manaBefore = player.currentMana;
    const handBefore = player.handCount;
    assert.throws(() => engine.playCard('game-1', 'p1', spell.id));
    assert.equal(player.currentMana, manaBefore);
    assert.equal(player.handCount, handBefore);
    assert.equal(player.findCardInHand(spell.id)?.id, spell.id);
  });

  it('Paladin hero power board đầy: không mất mana', () => {
    const engine = new GameEngine();
    const cards = Array.from({ length: 30 }, () => makeMinionCard({ manaCost: 0 }));
    const game = engine.createGame('paladin-game', 'PAL123', [
      { playerId: 'p1', name: 'One', hero: makeHero('PALADIN'), deck: new Deck(cards) },
      { playerId: 'p2', name: 'Two', hero: makeHero(), deck: new Deck(cards) },
    ]);
    game.start();
    const player = game.getPlayerById('p1');
    for (const c of [...player.handCards]) {
      player.removeFromHand(c.id);
    }
    for (let i = 0; i < MAX_BOARD_SIZE; i++) {
      const card = makeMinionCard({ manaCost: 0 });
      player.addToHand(card);
      engine.playCard('paladin-game', 'p1', card.id);
    }
    const manaBefore = player.currentMana;
    assert.throws(() => engine.useHeroPower('paladin-game', 'p1'));
    assert.equal(player.currentMana, manaBefore);
    assert.equal(player.boardCount, MAX_BOARD_SIZE);
  });

  it('finish idempotent: winner không bị ghi đè', () => {
    const { game } = setupEngine();
    game.finish('p1');
    game.finish('p2');
    assert.equal(game.getWinnerId(), 'p1');
  });

  it('tay giới hạn 6 lá: addToHand quá 6 bị burn', () => {
    const { game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    for (const c of [...p1.handCards]) {
      p1.removeFromHand(c.id);
    }
    for (let i = 0; i < 8; i++) {
      p1.addToHand(makeMinionCard({ manaCost: 0 }));
    }
    assert.equal(p1.handCount, 6);
  });

  it('rút bài thủ công: mỗi turn 1 lá, turn sau được rút tiếp', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    const before = p1.handCount;
    engine.drawCard('game-1', 'p1');
    assert.equal(p1.handCount, before + 1);
    assert.throws(() => engine.drawCard('game-1', 'p1'));
    engine.endTurn('game-1', 'p1');
    engine.endTurn('game-1', 'p2');
    const before2 = p1.handCount;
    engine.drawCard('game-1', 'p1');
    assert.equal(p1.handCount, before2 + 1);
  });

  it('rút bài khi tay đầy hoặc deck hết bị reject', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    for (const c of [...p1.handCards]) {
      p1.removeFromHand(c.id);
    }
    for (let i = 0; i < 6; i++) {
      p1.addToHand(makeMinionCard({ manaCost: 0 }));
    }
    assert.throws(() => engine.drawCard('game-1', 'p1'));
  });

  it('spell ENEMY_MINION: reject quái mình (nguyên tử), cho phép quái địch', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    const p2 = game.getPlayerById('p2');
    p1.summonMinion(new Minion('own-1', 'c', 'Own', 2, 5, 'p1', false, 'img'));
    p2.summonMinion(new Minion('foe-1', 'c', 'Foe', 2, 5, 'p2', false, 'img'));
    const curse = makeSpellCard({
      manaCost: 0,
      effects: [{ type: 'DAMAGE', value: 2, target: 'ENEMY_MINION' }],
    });
    p1.addToHand(curse);
    const manaBefore = p1.currentMana;
    const handBefore = p1.handCount;
    assert.throws(() => engine.playCard('game-1', 'p1', curse.id, 'own-1'));
    assert.equal(p1.handCount, handBefore);
    assert.equal(p1.currentMana, manaBefore);
    assert.equal(p1.findMinion('own-1')?.currentHealth, 5);
    engine.playCard('game-1', 'p1', curse.id, 'foe-1');
    assert.equal(p2.findMinion('foe-1')?.currentHealth, 3);
  });

  it('Siphon Soul: cast được — destroy đúng target, heal hero mình', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    const p2 = game.getPlayerById('p2');
    p2.summonMinion(new Minion('big-1', 'c', 'Big', 6, 6, 'p2', false, 'img'));
    p1.heroState.takeDamage(10);
    const siphon = makeSpellCard({
      manaCost: 0,
      effects: [
        { type: 'DESTROY', value: 0, target: 'ANY_MINION' },
        { type: 'HEAL', value: 3, target: 'FRIENDLY_HERO' },
      ],
    });
    p1.addToHand(siphon);
    engine.playCard('game-1', 'p1', siphon.id, 'big-1');
    assert.equal(p2.boardCount, 0);
    assert.equal(p1.heroState.currentHealth, 23);
  });

  it('attack: chặn dùng quái địch + tự đánh mình, fail không mất lượt', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    const p2 = game.getPlayerById('p2');
    const attacker = new Minion('atk-1', 'c', 'Atk', 3, 3, 'p1', true, 'img');
    const mate = new Minion('mate-1', 'c', 'Mate', 2, 2, 'p1', false, 'img');
    const foe = new Minion('foe-2', 'c', 'Foe', 2, 2, 'p2', true, 'img');
    p1.summonMinion(attacker);
    p1.summonMinion(mate);
    p2.summonMinion(foe);
    assert.throws(() => engine.attack('game-1', 'p1', 'foe-2', 'p2'));
    assert.throws(() => engine.attack('game-1', 'p1', 'atk-1', 'p1'));
    assert.throws(() => engine.attack('game-1', 'p1', 'atk-1', 'mate-1'));
    assert.equal(attacker.canAttack, true);
    engine.attack('game-1', 'p1', 'atk-1', 'p2');
    assert.equal(p2.heroState.currentHealth, 27);
    assert.equal(attacker.canAttack, false);
  });

  it('Taunt: chặn đánh hero/quái thường, cho đánh Taunt', () => {
    const { engine, game } = setupEngine();
    const p1 = game.getPlayerById('p1');
    const p2 = game.getPlayerById('p2');
    const attacker = new Minion('atk-2', 'c', 'Atk', 3, 5, 'p1', true, 'img');
    p1.summonMinion(attacker);
    p2.summonMinion(new Minion('taunt-1', 'c', 'Taunt', 2, 4, 'p2', false, 'img', true));
    p2.summonMinion(new Minion('plain-1', 'c', 'Plain', 2, 4, 'p2', false, 'img'));
    assert.throws(() => engine.attack('game-1', 'p1', 'atk-2', 'p2'));
    assert.throws(() => engine.attack('game-1', 'p1', 'atk-2', 'plain-1'));
    assert.equal(attacker.canAttack, true);
    engine.attack('game-1', 'p1', 'atk-2', 'taunt-1');
    assert.equal(p2.findMinion('taunt-1')?.currentHealth, 1);
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
