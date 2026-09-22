// Deterministic WebSocket scenarios for rules that need a real two-player match.
const { connect } = require('./e2e-ws.cjs');

const URL = process.env.GAME_URL || 'http://127.0.0.1:3000';

function waitEvent(socket, event, predicate = () => true, timeoutMs = 12_000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      socket.off(event, handler);
      reject(new Error(`timeout ${event}`));
    }, timeoutMs);
    const handler = (data) => {
      try {
        if (!predicate(data)) return;
        clearTimeout(timer);
        socket.off(event, handler);
        resolve(data);
      } catch (error) {
        clearTimeout(timer);
        socket.off(event, handler);
        reject(error);
      }
    };
    socket.on(event, handler);
  });
}

function check(label, condition) {
  if (!condition) throw new Error(`FAIL ${label}`);
  console.log(`  PASS ${label}`);
}

function playerIn(state, playerId) {
  const player = state.players.find((candidate) => candidate.playerId === playerId);
  if (!player) throw new Error(`missing player ${playerId}`);
  return player;
}

function buildDeck(catalog, heroClass, preferredSlugs) {
  const bySlug = new Map(catalog.collectibleCards.map((card) => [card.slug, card]));
  const copies = new Map();
  const deck = [];
  const add = (slug) => {
    const card = bySlug.get(slug);
    if (!card || (card.heroClass !== 'NEUTRAL' && card.heroClass !== heroClass)) {
      throw new Error(`illegal preferred card ${slug} for ${heroClass}`);
    }
    const limit = card.rarity === 'LEGENDARY' ? 1 : 2;
    const used = copies.get(slug) || 0;
    if (used >= limit || deck.length >= catalog.deckRules.deckSize) return;
    copies.set(slug, used + 1);
    deck.push(slug);
  };

  preferredSlugs.forEach(add);
  (catalog.suggestedDecks[heroClass] || []).forEach(add);
  if (deck.length !== catalog.deckRules.deckSize) {
    throw new Error(`could not build a ${catalog.deckRules.deckSize}-card ${heroClass} deck`);
  }
  return deck;
}

async function main() {
  const p1 = connect(URL);
  const p2 = connect(URL);
  const states = { p1: null, p2: null };
  try {
    await Promise.all([waitEvent(p1, 'connect'), waitEvent(p2, 'connect')]);
    const createdPromise = waitEvent(p1, 'ROOM_CREATED');
    p1.emit('CREATE_ROOM', { playerName: 'Rules Paladin' });
    const created = await createdPromise;
    const joinedPromise = waitEvent(p2, 'PLAYER_JOINED', (data) => Boolean(data.playerId));
    p2.emit('JOIN_ROOM', { roomCode: created.roomCode, playerName: 'Rules Mage' });
    const joined = await joinedPromise;

    const catalogResponse = await fetch(`${URL}/api/game-catalog`);
    if (!catalogResponse.ok) throw new Error(`catalog ${catalogResponse.status}`);
    const catalog = await catalogResponse.json();
    const paladinDeck = buildDeck(catalog, 'PALADIN', ['goldshire-footman']);
    const mageDeck = buildDeck(catalog, 'MAGE', ['stonetusk-boar', 'pyroblast']);

    const startedPromise = waitEvent(p1, 'GAME_STARTED');
    const initialP1 = waitEvent(p1, 'GAME_STATE_UPDATED', (data) => data.gameState.status === 'PLAYING');
    const initialP2 = waitEvent(p2, 'GAME_STATE_UPDATED', (data) => data.gameState.status === 'PLAYING');
    p1.emit('SUBMIT_LOADOUT', {
      roomCode: created.roomCode,
      heroClass: 'PALADIN',
      cardSlugs: paladinDeck,
    });
    p2.emit('SUBMIT_LOADOUT', {
      roomCode: created.roomCode,
      heroClass: 'MAGE',
      cardSlugs: mageDeck,
    });
    const started = await startedPromise;
    const gameId = started.gameId;
    states.p1 = (await initialP1).gameState;
    states.p2 = (await initialP2).gameState;
    const players = new Map([
      [created.playerId, { id: created.playerId, key: 'p1', socket: p1 }],
      [joined.playerId, { id: joined.playerId, key: 'p2', socket: p2 }],
    ]);
    const paladin = players.get(created.playerId);
    const mage = players.get(joined.playerId);

    const stateFor = (participant) => states[participant.key];
    const activeState = () => {
      const current = states.p1;
      const participant = players.get(current.activePlayerId);
      return stateFor(participant);
    };
    const update = (participant, payload) => {
      states[participant.key] = payload.gameState;
      return payload.gameState;
    };
    const endTurn = async () => {
      const before = activeState();
      const actor = players.get(before.activePlayerId);
      const p1Update = waitEvent(p1, 'GAME_STATE_UPDATED',
        (data) => data.gameState.gameId === gameId && data.gameState.turn > before.turn);
      const p2Update = waitEvent(p2, 'GAME_STATE_UPDATED',
        (data) => data.gameState.gameId === gameId && data.gameState.turn > before.turn);
      actor.socket.emit('END_TURN', { gameId });
      update(paladin, await p1Update);
      update(mage, await p2Update);
      return activeState();
    };
    const discardOne = async (actor, state) => {
      const own = playerIn(state, actor.id);
      const card = own.hand.find((candidate) => candidate.type === 'SPELL'
        && candidate.manaCost <= own.mana
        && candidate.effects.every((effect) => effect.target === 'FRIENDLY_HERO'));
      if (!card) return;
      const handCount = own.handCount;
      const played = waitEvent(actor.socket, 'GAME_STATE_UPDATED', (data) =>
        data.gameState.gameId === gameId && playerIn(data.gameState, actor.id).handCount < handCount);
      actor.socket.emit('PLAY_CARD', { gameId, cardInstanceId: card.id });
      update(actor, await played);
    };
    const advanceUntil = async (predicate, limit = 64) => {
      for (let turn = 0; turn < limit; turn += 1) {
        const state = activeState();
        const actor = players.get(state.activePlayerId);
        if (predicate(actor, state)) return { actor, state };
        await discardOne(actor, state);
        await endTurn();
      }
      throw new Error('rule setup exceeded turn limit');
    };

    // Paladin power must mutate once, then reject a second command in the same turn.
    console.log('  RULE hero power limit');
    let setup = await advanceUntil((actor, state) => actor.id === paladin.id
      && playerIn(state, paladin.id).mana >= playerIn(state, paladin.id).hero.powerCost * 2);
    let own = playerIn(setup.state, paladin.id);
    const powerUpdate = waitEvent(paladin.socket, 'GAME_STATE_UPDATED', (data) => {
      const next = playerIn(data.gameState, paladin.id);
      return data.gameState.gameId === gameId && next.heroPowerUsed && next.mana < own.mana;
    });
    paladin.socket.emit('USE_HERO_POWER', { gameId });
    update(paladin, await powerUpdate);
    const powerRejected = waitEvent(paladin.socket, 'ACTION_REJECTED',
      (data) => data.code === 'HERO_POWER_ALREADY_USED');
    paladin.socket.emit('USE_HERO_POWER', { gameId });
    await powerRejected;
    check('hero power only once per turn', true);

    // A Taunt minion prevents a Charge minion from attacking the hero directly.
    console.log('  RULE taunt');
    setup = await advanceUntil((actor, state) => actor.id === paladin.id
      && playerIn(state, paladin.id).hand.some((card) => card.slug === 'goldshire-footman'
        && card.manaCost <= playerIn(state, paladin.id).mana));
    own = playerIn(setup.state, paladin.id);
    const taunt = own.hand.find((card) => card.slug === 'goldshire-footman');
    const tauntUpdate = waitEvent(paladin.socket, 'GAME_STATE_UPDATED', (data) =>
      data.gameState.gameId === gameId
      && playerIn(data.gameState, paladin.id).board.length > own.board.length);
    paladin.socket.emit('PLAY_CARD', { gameId, cardInstanceId: taunt.id });
    update(paladin, await tauntUpdate);

    console.log('  RULE charge attack into taunt');
    setup = await advanceUntil((actor, state) => actor.id === mage.id
      && playerIn(state, mage.id).hand.some((card) => card.slug === 'stonetusk-boar'
        && card.manaCost <= playerIn(state, mage.id).mana));
    own = playerIn(setup.state, mage.id);
    const boar = own.hand.find((card) => card.slug === 'stonetusk-boar');
    const boarUpdate = waitEvent(mage.socket, 'GAME_STATE_UPDATED', (data) =>
      data.gameState.gameId === gameId && playerIn(data.gameState, mage.id).board.length > own.board.length);
    mage.socket.emit('PLAY_CARD', { gameId, cardInstanceId: boar.id });
    const mageAfterBoar = update(mage, await boarUpdate);
    const boarInstance = playerIn(mageAfterBoar, mage.id).board.find((minion) => minion.canAttack);
    const tauntRejected = waitEvent(mage.socket, 'ACTION_REJECTED',
      (data) => data.code === 'TAUNT_REQUIRED');
    mage.socket.emit('ATTACK', { gameId, attackerId: boarInstance.instanceId, targetId: paladin.id });
    await tauntRejected;
    check('Taunt blocks a hero attack', true);

    // ENEMY_CHARACTER must accept the opponent playerId when the chosen target is a hero.
    console.log('  RULE hero target');
    setup = await advanceUntil((actor, state) => actor.id === mage.id
      && playerIn(state, mage.id).hand.some((card) => card.slug === 'pyroblast'
        && card.manaCost <= playerIn(state, mage.id).mana));
    own = playerIn(setup.state, mage.id);
    const pyroblast = own.hand.find((card) => card.slug === 'pyroblast');
    const paladinHealth = playerIn(setup.state, paladin.id).hero.health;
    const heroTargetUpdate = waitEvent(mage.socket, 'GAME_STATE_UPDATED', (data) =>
      data.gameState.gameId === gameId
      && playerIn(data.gameState, paladin.id).hero.health === paladinHealth - 10);
    mage.socket.emit('PLAY_CARD', {
      gameId,
      cardInstanceId: pyroblast.id,
      targetId: paladin.id,
    });
    update(mage, await heroTargetUpdate);
    check('spell targets hero by playerId', true);

    // Exhaust the deck naturally; the first failed draw must advance fatigue state.
    console.log('  RULE fatigue');
    let fatigued = null;
    for (let turn = 0; turn < 64 && fatigued === null; turn += 1) {
      const state = activeState();
      fatigued = state.players.find((player) => player.fatigueDamage > 0) || null;
      if (fatigued === null) await endTurn();
    }
    check('empty deck applies increasing fatigue', fatigued !== null && fatigued.fatigueDamage >= 1);

    // Concede is a real game-over command; the next two rematch votes start a fresh aggregate.
    console.log('  RULE game over and rematch');
    const gameOverP1 = waitEvent(p1, 'GAME_OVER');
    const gameOverP2 = waitEvent(p2, 'GAME_OVER');
    paladin.socket.emit('CONCEDE', { gameId });
    const [gameOverForP1, gameOverForP2] = await Promise.all([gameOverP1, gameOverP2]);
    check('game over reaches both players', gameOverForP1.winnerId === mage.id
      && gameOverForP2.winnerId === mage.id);

    const rematchStarted = waitEvent(p1, 'GAME_STARTED', (data) => data.gameId !== gameId, 20_000);
    const rematchP1 = waitEvent(p1, 'GAME_STATE_UPDATED',
      (data) => data.gameState.gameId !== gameId && data.gameState.status === 'PLAYING', 20_000);
    const rematchP2 = waitEvent(p2, 'GAME_STATE_UPDATED',
      (data) => data.gameState.gameId !== gameId && data.gameState.status === 'PLAYING', 20_000);
    paladin.socket.emit('REMATCH', { gameId });
    mage.socket.emit('REMATCH', { gameId });
    const newGame = await rematchStarted;
    const [newStateP1, newStateP2] = await Promise.all([rematchP1, rematchP2]);
    check('rematch starts a fresh shuffled game', newGame.gameId === newStateP1.gameState.gameId
      && newGame.gameId === newStateP2.gameState.gameId);
  } finally {
    p1.disconnect();
    p2.disconnect();
  }
}

main().catch((error) => {
  console.error('RULES E2E ERROR:', error.message);
  process.exitCode = 1;
});
