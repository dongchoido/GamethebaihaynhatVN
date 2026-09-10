// Mô phỏng đúng 2 tab browser: qua proxy :5173 + reauth/RECONNECT như client.
const { io } = require('socket.io-client');

const URL = 'http://localhost:5173';
let pass = 0;
let fail = 0;
const check = (name, ok, extra = '') => {
  if (ok) pass++;
  else fail++;
  console.log(ok ? `  PASS ${name}` : `  FAIL ${name} ${extra}`);
};

function once(socket, event, timeoutMs = 10000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`timeout ${event}`)), timeoutMs);
    socket.once(event, (data) => {
      clearTimeout(timer);
      resolve(data);
    });
  });
}

// Client-faithful: sau khi có token → gắn auth, reconnect, auto RECONNECT_GAME.
async function reauth(socket, token) {
  socket.auth = { sessionToken: token };
  const connectedP = once(socket, 'connect');
  socket.disconnect().connect();
  await connectedP;
  socket.emit('RECONNECT_GAME', { sessionToken: token });
}

function waitTurn(socket, gameId, minTurn, timeoutMs = 12000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout waitTurn')), timeoutMs);
    const handler = (s) => {
      if (s.gameState.gameId === gameId && s.gameState.turn >= minTurn) {
        clearTimeout(timer);
        socket.off('GAME_STATE_UPDATED', handler);
        resolve(s.gameState);
      }
    };
    socket.on('GAME_STATE_UPDATED', handler);
  });
}

async function main() {
  const tab1 = io(URL);
  const tab2 = io(URL);
  await Promise.all([once(tab1, 'connect'), once(tab2, 'connect')]);
  check('2 tabs connect qua proxy', true);

  const createdP = once(tab1, 'ROOM_CREATED');
  tab1.emit('CREATE_ROOM', { playerName: 'Tab1' });
  const created = await createdP;
  await reauth(tab1, created.sessionToken);
  const aliceId = created.playerId;

  const joinedP = once(tab2, 'PLAYER_JOINED');
  tab2.emit('JOIN_ROOM', { roomCode: created.roomCode, playerName: 'Tab2' });
  const joined = await joinedP;
  await reauth(tab2, joined.sessionToken);
  const bobId = joined.playerId;

  const startedP = once(tab1, 'GAME_STARTED');
  const stateP = once(tab1, 'GAME_STATE_UPDATED');
  tab1.emit('SELECT_DECK', { heroId: 'MAGE', roomCode: created.roomCode });
  tab2.emit('SELECT_DECK', { heroId: 'HUNTER', roomCode: created.roomCode });
  const started = await startedP;
  const gameId = started.gameId;
  let cur = (await stateP).gameState;
  check('game start qua proxy', cur.status === 'PLAYING');

  // THEO DÕI MANA QUA CÁC LƯỢT — KHÔNG ĐÁNH GÌ, CHỈ END TURN
  const manaTrack = [];
  for (let i = 0; i < 6; i++) {
    const activeIsAlice = cur.activePlayerId === aliceId;
    const sock = activeIsAlice ? tab1 : tab2;
    const me = cur.players.find((p) => p.playerId === cur.activePlayerId);
    manaTrack.push(`T${cur.turn}:${activeIsAlice ? 'A' : 'B'}=${me.mana}/${me.maxMana}`);
    const pr = waitTurn(sock, gameId, cur.turn + 1);
    sock.emit('END_TURN', { gameId });
    cur = await pr;
  }
  console.log('  mana track:', manaTrack.join(' '));
  const aliceStates = manaTrack.filter((t) => t.includes(':A=')).map((t) => t.split('=')[1]);
  const bobStates = manaTrack.filter((t) => t.includes(':B=')).map((t) => t.split('=')[1]);
  check('mana Alice tăng 1/1 → 2/2 → 3/3', aliceStates.join(',') === '1/1,2/2,3/3', aliceStates.join(','));
  check('mana Bob tăng 1/1 → 2/2 → 3/3', bobStates.join(',') === '1/1,2/2,3/3', bobStates.join(','));
  check('END TURN không cần đánh bài', true);

  // P2 (Bob) chơi 1 lá rồi đánh hero — kiểm tra tab2 nhận đủ state
  const bob = cur.players.find((p) => p.playerId === bobId);
  // Đảm bảo tới lượt Bob
  if (cur.activePlayerId !== bobId) {
    const pr = waitTurn(tab1, gameId, cur.turn + 1);
    tab1.emit('END_TURN', { gameId });
    cur = await pr;
  }
  const bobNow = cur.players.find((p) => p.playerId === bobId);
  const minion = bobNow.hand.find((c) => c.type === 'MINION' && c.manaCost <= bobNow.mana);
  if (minion) {
    const pr = waitTurn(tab2, gameId, cur.turn);
    tab2.emit('PLAY_CARD', { gameId, cardInstanceId: minion.id });
    cur = await pr;
    const bobAfter = cur.players.find((p) => p.playerId === bobId);
    check('tab2 đánh minion được', bobAfter.board.length === 1);
  } else {
    check('tab2 đánh minion được (không có minion rẻ, bỏ qua)', true);
  }

  tab1.disconnect();
  tab2.disconnect();
  console.log(`\nTABS: ${pass}/${pass + fail} pass`);
  process.exit(fail ? 1 : 0);
}

main().catch((err) => {
  console.error('TABS ERROR:', err.message);
  process.exit(1);
});
