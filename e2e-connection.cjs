const assert = require('node:assert/strict');
const { connect } = require('./e2e-ws.cjs');
const { deckFor } = require('./e2e-deck.cjs');
const url = process.env.GAME_URL || 'http://127.0.0.1:3000';
const io = (u, opts) => connect(u, opts);
function wait(socket, event, predicate = () => true) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { socket.off(event, handler); reject(new Error(`Timeout: ${event}`)); }, 15000);
    function handler(value) {
      if (!predicate(value)) return;
      clearTimeout(timer); socket.off(event, handler); resolve(value);
    }
    socket.on(event, handler);
  });
}
async function main() {
  const a = io(url, { autoConnect: false });
  let b = io(url, { autoConnect: false });
  const errors = [];
  let disconnects = 0;
  a.on('PLAYER_DISCONNECTED', () => disconnects++);
  a.on('ACTION_REJECTED', e => { errors.push(e); console.error(e); });
  b.on('ACTION_REJECTED', e => { errors.push(e); console.error(e); });
  try {
    const connected = Promise.all([wait(a, 'connect'), wait(b, 'connect')]);
    a.connect(); b.connect(); await connected;
    let response = wait(a, 'ROOM_CREATED');
    a.emit('CREATE_ROOM', { playerName: 'Connection test A' });
    const created = await response;
    response = wait(b, 'PLAYER_JOINED', r => !!r.sessionToken);
    b.emit('JOIN_ROOM', { playerName: 'Connection test B', roomCode: created.roomCode });
    const joined = await response;
    // Like the browser, save auth without disconnecting either socket.
    a.auth = { sessionToken: created.sessionToken };
    b.auth = { sessionToken: joined.sessionToken };
    const states = Promise.all([wait(a, 'GAME_STATE_UPDATED'), wait(b, 'GAME_STATE_UPDATED')]);
    const [mageDeck, hunterDeck] = await Promise.all([deckFor(url, 'MAGE'), deckFor(url, 'HUNTER')]);
    a.emit('SUBMIT_LOADOUT', { roomCode: created.roomCode, heroClass: 'MAGE', cardSlugs: mageDeck });
    b.emit('SUBMIT_LOADOUT', { roomCode: created.roomCode, heroClass: 'HUNTER', cardSlugs: hunterDeck });
    const [first, second] = await states;
    assert.equal(first.gameState.gameId, second.gameState.gameId);
    assert.equal(first.gameState.status, 'PLAYING');
    assert.equal(disconnects, 0, 'Joining must not disconnect the opponent');
    console.log('PASS: join and start without reconnect/auth race');
    const left = wait(a, 'PLAYER_DISCONNECTED'); b.disconnect(); await left;
    b = io(url, { autoConnect: false, auth: { sessionToken: joined.sessionToken } });
    const back = wait(a, 'PLAYER_JOINED');
    const restored = wait(b, 'GAME_STATE_UPDATED');
    b.on('connect', () => b.emit('RECONNECT_GAME', { sessionToken: joined.sessionToken }));
    b.connect(); await back;
    const restoredState = (await restored).gameState;
    assert.equal(restoredState.gameId, first.gameState.gameId);
    const socketFor = (playerId) => playerId === created.playerId ? a : b;
    const next = wait(a, 'GAME_STATE_UPDATED', r => r.gameState.turn === 2);
    socketFor(restoredState.activePlayerId).emit('END_TURN', { gameId: first.gameState.gameId });
    const turnTwo = await next;
    const third = wait(a, 'GAME_STATE_UPDATED', r => r.gameState.turn === 3);
    socketFor(turnTwo.gameState.activePlayerId).emit('END_TURN', { gameId: first.gameState.gameId });
    await third;
    assert.equal(errors.length, 0);
    console.log('PASS: opponent notified on reconnect; both players can take turns');
  } finally { a.disconnect(); b.disconnect(); }
}
main().catch(e => { console.error(e); process.exitCode = 1; });
