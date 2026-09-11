import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { type CardDefinition, CardType } from '@coincard/shared';
import { GameEngine } from './GameEngine.js';
import { Deck } from './Deck.js';
import { Hero } from './Hero.js';
import { Minion } from './Minion.js';
import { Player } from './Player.js';

const catalog: CardDefinition[] = JSON.parse(readFileSync(new URL('../../../data/cards.json', import.meta.url), 'utf8'));
const card = (slug: string): CardDefinition => {
  const found = catalog.find(c => c.slug === slug);
  assert.ok(found, slug);
  return { ...structuredClone(found), id: slug, keywords: found.keywords ?? [], collectible: true };
};
function setup() {
  const engine = new GameEngine();
  const game = engine.createGame('test', 'ABC123', ['a', 'b'].map(playerId => ({
    playerId, name: playerId, hero: new Hero(playerId, playerId, 'MAGE', 'Fireblast', 2, ''),
    deck: new Deck(Array.from({ length: 30 }, (_, i) => ({ ...card(catalog[0]!.slug), id: `${playerId}-${i}` }))),
  })) as Parameters<GameEngine['createGame']>[2]);
  game.start();
  const [a, b] = game.getPlayers();
  assert.ok(a && b);
  for (let i = 0; i < 10; i++) a.increaseMaxMana();
  a.refillMana();
  return { engine, game, a, b };
}
const unit = (p: Player, id: string, attack = 2) => {
  const m = new Minion(id, id, id, attack, 2, p.id, true, '');
  p.summonMinion(m); return m;
};

test('real catalog: every spell resolves on a valid target', () => {
  for (const source of catalog.filter(c => c.type === CardType.SPELL)) {
    const { engine, a, b } = setup();
    const own = unit(a, 'own', 8); const enemy = unit(b, 'enemy', 8);
    const c = card(source.slug); a.addToHand(c);
    const targeting = c.effects.find(e => ['ANY_MINION', 'ENEMY_MINION', 'FRIENDLY_MINION', 'ANY_CHARACTER', 'ENEMY_CHARACTER', 'ENEMY_HERO'].includes(e.target));
    const target = targeting?.target === 'FRIENDLY_MINION' ? own.instanceId
      : targeting?.target === 'ENEMY_HERO' ? b.id : targeting ? enemy.instanceId : undefined;
    assert.doesNotThrow(() => engine.playCard('test', a.id, c.id, target), c.slug);
    assert.equal(a.cardsPlayed, 1, c.slug);
  }
});
test('Polymorph replaces a slot on a full board without increasing summons', () => {
  const { engine, a, b } = setup();
  for (let i = 0; i < 7; i++) unit(b, `enemy-${i}`);
  const c = card('polymorph'); a.addToHand(c);
  engine.playCard('test', a.id, c.id, 'enemy-3');
  assert.equal(b.boardCount, 7); assert.equal(b.minionsSummoned, 7);
  assert.equal(b.getBoard()[3]?.currentHealth, 1);
});
test('Siphon Soul destroys low attack minions and heals its owner', () => {
  const { engine, a, b } = setup(); unit(b, 'target', 1); a.hero.takeDamage(8);
  const c = card('siphon-soul'); a.addToHand(c);
  engine.playCard('test', a.id, c.id, 'target');
  assert.equal(b.boardCount, 0); assert.equal(a.hero.currentHealth, 25);
});
test('conditional DESTROY rejects before spending card/mana', () => {
  const { engine, a, b } = setup(); unit(b, 'target', 1);
  const c = card('siphon-soul'); c.effects = [{ type: 'DESTROY', value: 0, target: 'ENEMY_MINION', minAttack: 5 }]; a.addToHand(c);
  const before = a.handCards;
  assert.throws(() => engine.playCard('test', a.id, c.id, 'target'));
  assert.deepEqual(a.handCards, before); assert.equal(a.currentMana, 10);
});
test('runtime failure in a later effect rolls back board, hand, mana and statistics', () => {
  const { engine, a, b } = setup(); const target = unit(b, 'target');
  const c = card('polymorph'); c.effects.push({ type: 'DAMAGE', value: 2, target: 'ANY_MINION' }); a.addToHand(c);
  const before = a.handCards;
  assert.throws(() => engine.playCard('test', a.id, c.id, 'target'));
  assert.deepEqual(a.handCards, before); assert.equal(a.currentMana, 10); assert.equal(a.cardsPlayed, 0);
  assert.equal(b.getBoard()[0], target); assert.equal(target.currentHealth, 2);
});
test('opening guarantee without cheap cards conserves deck and unique instances', () => {
  const { a } = setup(); const initial = a.handCount + a.deckSize;
  a.guaranteeCheapOpener(1); a.guaranteeCheapOpener(1);
  assert.equal(a.handCount + a.deckSize, initial);
  const ids = a.handCards.map(c => c.id);
  while (a.deckSize) ids.push(a.drawCard().id);
  assert.equal(new Set(ids).size, initial);
});
test('both heroes dying within one action is a draw', () => {
  const { engine, game, a } = setup(); const c = card('siphon-soul');
  c.effects = [{ type: 'DAMAGE', value: 30, target: 'ENEMY_HERO' }, { type: 'DAMAGE', value: 30, target: 'FRIENDLY_HERO' }];
  a.addToHand(c); engine.playCard('test', a.id, c.id);
  assert.equal(game.getStatus(), 'FINISHED'); assert.equal(game.getWinnerId(), null);
});
test('overkill and retaliation count actual health removed for both players', () => {
  const { engine, a, b } = setup(); unit(a, 'attacker', 9); unit(b, 'defender', 8);
  engine.attack('test', a.id, 'attacker', 'defender');
  assert.equal(a.damageDealt, 2); assert.equal(b.damageDealt, 2);
});
test('hand definitions are defensive copies and invalid damage cannot heal', () => {
  const { a } = setup(); const hand = a.handCards; hand[0]!.manaCost = -20;
  assert.notEqual(a.handCards[0]!.manaCost, -20);
  assert.throws(() => a.hero.takeDamage(-1)); assert.equal(a.hero.currentHealth, 30);
});
