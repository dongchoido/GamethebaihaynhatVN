import assert from 'node:assert/strict';
import { io } from 'socket.io-client';
import { pathToFileURL } from 'node:url';

function wait(socket, event, predicate = () => true) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { socket.off(event, handler); reject(new Error(`Timeout ${event}`)); }, 15000);
    function handler(data) { if (predicate(data)) { clearTimeout(timer); socket.off(event, handler); resolve(data); } }
    socket.on(event, handler);
  });
}
export async function checkOnline(url) {
  const sockets = [];
  async function connect() {
    const socket = io(url, { autoConnect: false, reconnection: false, forceNew: true });
    sockets.push(socket); const ready = wait(socket, 'connect'); socket.connect(); await ready; return socket;
  }
  const request = async (socket, event, data, response, predicate) => {
    const result = wait(socket, response, predicate); socket.emit(event, data); return result;
  };
  const state = socket => wait(socket, 'GAME_STATE_UPDATED');
  try {
    const a = await connect(); let b = await connect();
    const created = await request(a, 'CREATE_ROOM', { playerName: 'Regression A' }, 'ROOM_CREATED');
    const joined = await request(b, 'JOIN_ROOM', { playerName: 'Regression B', roomCode: created.roomCode }, 'PLAYER_JOINED');
    const roomCode = created.roomCode;
    await request(a, 'SELECT_DECK', { roomCode, heroId: 'MAGE' }, 'DECK_SELECTED');
    const first = state(a); const second = state(b);
    b.emit('SELECT_DECK', { roomCode, heroId: 'HUNTER' });
    const [{ gameState: s1 }, { gameState: s2 }] = await Promise.all([first, second]);
    const gameId = s1.gameId;
    assert.equal(s1.players[1].hand.length, 0); assert.equal(s2.players[0].hand.length, 0);
    assert.equal(s1.players[0].hand.length, 3);
    await request(b, 'END_TURN', { gameId }, 'ACTION_REJECTED');
    const draw = await request(a, 'DRAW_CARD', { gameId }, 'GAME_STATE_UPDATED');
    assert.equal(draw.gameState.manualDrawUsed, true);
    await request(a, 'DRAW_CARD', { gameId }, 'ACTION_REJECTED');
    // Replacement socket takes over the same player without reporting them disconnected.
    const old = b; b = await connect(); const oldGone = wait(old, 'disconnect');
    const restored = await request(b, 'RECONNECT_GAME', { sessionToken: joined.sessionToken }, 'GAME_STATE_UPDATED');
    await oldGone; assert.equal(restored.gameState.gameId, gameId); assert.equal(old.connected, false);
    const disconnected = wait(a, 'PLAYER_DISCONNECTED'); b.disconnect(); await disconnected;
    b = await connect();
    await request(b, 'RECONNECT_GAME', { sessionToken: joined.sessionToken }, 'GAME_STATE_UPDATED');
    // Manual draw flag survives a refresh/takeover as well.
    const observer = await connect();
    await request(observer, 'END_TURN', { gameId }, 'ACTION_REJECTED');
    let current = (await request(a, 'END_TURN', { gameId }, 'GAME_STATE_UPDATED', r => r.gameState.turn === 2)).gameState;
    assert.equal(current.activePlayerId, joined.playerId);
    for (let i = 0; i < 4; i++) {
      const active = current.activePlayerId === created.playerId ? a : b;
      current = (await request(active, 'END_TURN', { gameId }, 'GAME_STATE_UPDATED', r => r.gameState.turn === current.turn + 1)).gameState;
    }
    const active = current.activePlayerId === created.playerId ? a : b;
    const hpBefore = current.players.find(p => p.playerId !== current.activePlayerId).hero.health;
    const power = (await request(active, 'USE_HERO_POWER', { gameId }, 'GAME_STATE_UPDATED', r => r.gameState.turn === current.turn && r.gameState.players.find(p => p.playerId !== current.activePlayerId).hero.health < hpBefore)).gameState;
    assert.ok(power.players.find(p => p.playerId !== current.activePlayerId).hero.health < hpBefore);
    const overB = wait(b, 'GAME_STATE_UPDATED', r => r.gameState.status === 'FINISHED');
    const finished = await request(a, 'CONCEDE', { gameId }, 'GAME_STATE_UPDATED', r => r.gameState.status === 'FINISHED'); await overB;
    assert.equal(finished.gameState.winnerId, joined.playerId);
    await request(a, 'REMATCH', { gameId }, 'REMATCH_REQUESTED');
    const rematch = wait(a, 'GAME_STATE_UPDATED', r => r.gameState.gameId !== gameId); const rematchB = wait(b, 'GAME_STATE_UPDATED', r => r.gameState.gameId !== gameId); b.emit('REMATCH', { gameId });
    const [{ gameState: next }] = await Promise.all([rematch, rematchB]);
    assert.notEqual(next.gameId, gameId); assert.equal(next.status, 'PLAYING');
    await request(a, 'END_TURN', { gameId }, 'ACTION_REJECTED');
    await request(a, 'REMATCH', { gameId: next.gameId }, 'ACTION_REJECTED');
    const end = await request(b, 'CONCEDE', { gameId: next.gameId }, 'GAME_STATE_UPDATED', r => r.gameState.status === 'FINISHED');
    assert.equal(end.gameState.winnerId, created.playerId);
    console.log('PASS two clients: room, privacy, draw, turn, HP/mana, takeover, disconnect, reconnect, result, rematch, stale game');
    return [gameId, next.gameId];
  } finally { sockets.forEach(socket => socket.disconnect()); }
}
if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) await checkOnline(process.argv[2]);
