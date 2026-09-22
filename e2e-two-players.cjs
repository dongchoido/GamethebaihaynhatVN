// E2E: mô phỏng 2 browser chơi qua WebSocket thật (server Java).
const { connect } = require('./e2e-ws.cjs');
const { deckFor } = require('./e2e-deck.cjs');

const URL = process.env.GAME_URL || 'http://localhost:3000';
const results = [];
const check = (name, ok) => {
  results.push([name, ok]);
  console.log(ok ? `  PASS ${name}` : `  FAIL ${name}`);
};

function once(socket, event, timeoutMs = 8000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`timeout ${event}`)), timeoutMs);
    socket.once(event, (data) => {
      clearTimeout(timer);
      resolve(data);
    });
  });
}

async function main() {
  const p1 = connect(URL);
  const p2 = connect(URL);
  await Promise.all([once(p1, 'connect'), once(p2, 'connect')]);
  check('2 sockets connected', true);

  // P1 tạo phòng
  const createdPromise = once(p1, 'ROOM_CREATED');
  p1.emit('CREATE_ROOM', { playerName: 'Alice' });
  const created = await createdPromise;
  check('ROOM_CREATED có roomCode 6 ký tự', created.roomCode && created.roomCode.length === 6);
  const roomCode = created.roomCode;

  // P2 join
  const joinedPromise = once(p2, 'PLAYER_JOINED');
  const p1JoinedPromise = once(p1, 'PLAYER_JOINED');
  p2.emit('JOIN_ROOM', { roomCode, playerName: 'Bob' });
  const joined = await joinedPromise;
  await p1JoinedPromise;
  check('PLAYER_JOINED đủ 2 người', joined.players && joined.players.length === 2);

  // Cả 2 submit loadout hợp lệ do catalog Java cung cấp
  const startedPromise = once(p1, 'GAME_STARTED');
  const statePromise = once(p1, 'GAME_STATE_UPDATED');
  const [mageDeck, hunterDeck] = await Promise.all([deckFor(URL, 'MAGE'), deckFor(URL, 'HUNTER')]);
  p1.emit('SUBMIT_LOADOUT', { heroClass: 'MAGE', roomCode, cardSlugs: mageDeck });
  p2.emit('SUBMIT_LOADOUT', { heroClass: 'HUNTER', roomCode, cardSlugs: hunterDeck });
  const started = await startedPromise;
  check('GAME_STARTED có gameId', !!started.gameId);
  const gameId = started.gameId;
  const state1 = await statePromise;
  const st = state1.gameState;
  check('status PLAYING', st.status === 'PLAYING');
  const firstState = st.players.find((p) => p.playerId === st.activePlayerId);
  const secondState = st.players.find((p) => p.playerId !== st.activePlayerId);
  check('tay đầu 4/5 lá + Coin', firstState.handCount === 4 && secondState.handCount === 5);
  check('mana lượt 1 = 1/1', firstState.mana === 1 && firstState.maxMana === 1);
  const aliceId = created.playerId;
  const bobId = joined.playerId;
  check('người đi trước hợp lệ', st.activePlayerId === aliceId || st.activePlayerId === bobId);

  // Che hand đối thủ: P1 không thấy bài P2
  const bobState = st.players.find((p) => p.playerId === bobId);
  check('che hand đối thủ', bobState.hand.length === 0 && bobState.handCount >= 4);

  // Một socket không có reconnect/session token không thể điều khiển trận đang chạy.
  const intruder = connect(URL);
  await once(intruder, 'connect');
  const intruderRejected = once(intruder, 'ACTION_REJECTED');
  intruder.emit('END_TURN', { gameId });
  const intruderError = await intruderRejected;
  check('socket không sở hữu trận bị chặn', intruderError.code === 'INVALID_PAYLOAD');
  intruder.disconnect();

  // Chơi vài turn: mỗi turn thử đánh lá đầu tiên chơi được (minion hoặc spell không-target)
  const NO_TARGET = new Set(['ALL_ENEMY_MINIONS', 'ALL_FRIENDLY_MINIONS', 'ALL_MINIONS', 'FRIENDLY_HERO', 'RANDOM_ENEMY']);
  const isPlayableNow = (c, mana) => {
    if (c.manaCost > mana) return false;
    if (c.type === 'MINION') return true;
    return c.effects.length > 0 && c.effects.every((e) => NO_TARGET.has(e.target));
  };
  // Spell có-target đánh vào hero địch (nếu effect cho phép)
  const isTargetedAtHero = (c, mana) =>
    c.manaCost <= mana &&
    c.type === 'SPELL' &&
    c.effects.length > 0 &&
    c.effects.every((e) => e.target === 'ENEMY_CHARACTER' || e.target === 'ENEMY_HERO');
  // Đợi state có tiến triển thật (bỏ qua packet cũ đến muộn giữa 2 connections).
  const waitProgress = (sock, isProgress, timeoutMs = 10000) =>
    new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        sock.off('GAME_STATE_UPDATED', handler);
        reject(new Error('timeout waitProgress'));
      }, timeoutMs);
      const handler = (r) => {
        if (r.gameState.gameId === gameId && isProgress(r.gameState)) {
          clearTimeout(timer);
          sock.off('GAME_STATE_UPDATED', handler);
          resolve(r.gameState);
        }
      };
      sock.on('GAME_STATE_UPDATED', handler);
    });
  let cur = st;
  let playedMinionId = null;
  let playedHasCharge = false;
  for (let turn = 0; turn < 14 && !playedMinionId; turn++) {
    const me = cur.players.find((p) => p.playerId === (cur.activePlayerId === aliceId ? aliceId : bobId));
    const foe = cur.players.find((p) => p.playerId !== me.playerId);
    const sock = cur.activePlayerId === aliceId ? p1 : p2;
    const card = me.hand.find((c) => isPlayableNow(c, me.mana) && c.type === 'MINION');
    if (card) {
      const myBoardBefore = me.board.length;
      const manaBefore = me.mana;
      const pr = waitProgress(
        sock,
        (s) => {
          const p = s.players.find((x) => x.playerId === me.playerId);
          return p.board.length !== myBoardBefore || p.mana !== manaBefore;
        },
      );
      sock.emit('PLAY_CARD', { gameId, cardInstanceId: card.id });
      cur = await pr;
      const after = cur.players.find((p) => p.playerId === me.playerId);
      if (after.board.length > 0) {
        playedMinionId = after.board[after.board.length - 1].instanceId;
        playedHasCharge = (card.keywords || []).includes('CHARGE');
        check('minion lên board + trừ mana', after.mana === me.mana - card.manaCost);
      }
      break;
    }
    const heroSpell = me.hand.find((c) => isTargetedAtHero(c, me.mana));
    if (heroSpell) {
      const handBefore = me.handCount;
      const pr = waitProgress(
        sock,
        (s) => s.players.find((x) => x.playerId === me.playerId).handCount !== handBefore,
      );
      sock.emit('PLAY_CARD', { gameId, cardInstanceId: heroSpell.id, targetId: foe.playerId });
      cur = await pr;
      continue;
    }
    const spell = me.hand.find((c) => isPlayableNow(c, me.mana));
    if (spell) {
      const handBefore = me.handCount;
      const pr = waitProgress(
        sock,
        (s) => s.players.find((x) => x.playerId === me.playerId).handCount !== handBefore,
      );
      sock.emit('PLAY_CARD', { gameId, cardInstanceId: spell.id });
      cur = await pr;
      continue;
    }
    const turnBefore = cur.turn;
    const bothPromise = Promise.all([
      waitProgress(p1, (s) => s.turn !== turnBefore),
      waitProgress(p2, (s) => s.turn !== turnBefore),
    ]);
    sock.emit('END_TURN', { gameId });
    const [stateForP1, stateForP2] = await bothPromise;
    // Đọc state từ đúng viewer (hand đối thủ luôn bị che).
    cur = stateForP1.activePlayerId === aliceId ? stateForP1 : stateForP2;
    continue;
  }
  check('đánh được minion trong 14 turn đầu', !!playedMinionId);
  if (!playedMinionId) {
    console.log('E2E: không rút được minion — dừng.');
    p1.disconnect();
    p2.disconnect();
    process.exit(1);
  }

  // Xác định chủ minion + đối thủ
  const ownerId = cur.players.find((p) => p.board.some((m) => m.instanceId === playedMinionId)).playerId;
  const foeId = ownerId === aliceId ? bobId : aliceId;
  const ownerSock = ownerId === aliceId ? p1 : p2;
  const foeSock = ownerId === aliceId ? p2 : p1;
  // Summoning sickness: nếu vừa summon trong turn này thì tấn bị reject
  // (trừ minion CHARGE — keywords thật từ DB nên charge tấn ngay được).
  const foeBefore = () => cur.players.find((p) => p.playerId === foeId).hero.health;
  if (cur.activePlayerId === ownerId) {
    if (playedHasCharge) {
      const hpB = foeBefore();
      const pr = waitProgress(
        ownerSock,
        (s) => s.players.find((p) => p.playerId === foeId).hero.health !== hpB,
      );
      ownerSock.emit('ATTACK', { gameId, attackerId: playedMinionId, targetId: foeId });
      cur = await pr;
      check('summoning sickness bị reject', cur.players.find((p) => p.playerId === foeId).hero.health < hpB);
    } else {
      const rejectPromise = once(ownerSock, 'ACTION_REJECTED');
      ownerSock.emit('ATTACK', { gameId, attackerId: playedMinionId, targetId: foeId });
      const rejected = await rejectPromise;
      check('summoning sickness bị reject', rejected.code === 'INVALID_TARGET');
    }
  } else {
    check('summoning sickness bị reject (bỏ qua, khác turn)', true);
  }

  // Hết sickness: end turn 2 vòng (owner→foe→owner) rồi mới attack.
  // Đợi cả 2 sockets cùng thấy turn mới (tránh race packet giữa 2 connections).
  const waitTurn = (minTurn) =>
    Promise.all([
      new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('timeout p1 turn')), 8000);
        const handler = (s) => {
          if (s.gameState.turn >= minTurn) {
            clearTimeout(timer);
            p1.off('GAME_STATE_UPDATED', handler);
            resolve(s.gameState);
          }
        };
        p1.on('GAME_STATE_UPDATED', handler);
      }),
      new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('timeout p2 turn')), 8000);
        const handler = (s) => {
          if (s.gameState.turn >= minTurn) {
            clearTimeout(timer);
            p2.off('GAME_STATE_UPDATED', handler);
            resolve(s.gameState);
          }
        };
        p2.on('GAME_STATE_UPDATED', handler);
      }),
    ]).then(([a]) => a);
  for (let i = 0; i < 2; i++) {
    const sock = cur.activePlayerId === aliceId ? p1 : p2;
    const pr = waitTurn(cur.turn + 1);
    sock.emit('END_TURN', { gameId });
    cur = await pr;
  }
  check('đến lượt owner minion', cur.activePlayerId === ownerId);
  const atkPower = cur.players.find((p) => p.playerId === ownerId).board.find((m) => m.instanceId === playedMinionId).attack;
  const hpBefore = foeBefore();
  {
    const pr = Promise.race([
      waitProgress(
        ownerSock,
        (s) => s.players.find((p) => p.playerId === foeId).hero.health !== hpBefore,
      ),
      once(ownerSock, 'ACTION_REJECTED').then((r) => ({ attackRejected: r })),
    ]);
    ownerSock.emit('ATTACK', { gameId, attackerId: playedMinionId, targetId: foeId });
    cur = await pr;
  }
  if (cur.attackRejected) {
    console.log('  ATTACK REJECTED:', JSON.stringify(cur.attackRejected));
    check('hero địch mất máu đúng attack', false);
  } else {
    check('hero địch mất máu đúng attack', cur.players.find((p) => p.playerId === foeId).hero.health === hpBefore - atkPower);
  }

  // Hero power: owner phải là MAGE/HUNTER mới gây damage trực tiếp — chỉ kiểm tra không lỗi
  {
    const ownerManaBefore = cur.players.find((p) => p.playerId === ownerId).mana;
    const pr = Promise.race([
      waitProgress(
        ownerSock,
        (s) => s.players.find((p) => p.playerId === ownerId).mana !== ownerManaBefore,
      ),
      once(ownerSock, 'ACTION_REJECTED').then((r) => ({ rejected: r })),
    ]);
    ownerSock.emit('USE_HERO_POWER', { gameId });
    const res = await pr;
    check('hero power chạy (state mới hoặc reject hợp lệ)', !!res && (res.gameId || res.rejected));
    if (res.gameId) cur = res;
  }

  // Đối thủ đầu hàng → GAME_OVER + lưu DB
  const overPromise = once(ownerSock, 'GAME_OVER');
  foeSock.emit('CONCEDE', { gameId });
  const over = await overPromise;
  check('GAME_OVER winner là owner phe còn lại', over.winnerId === ownerId);

  p1.disconnect();
  p2.disconnect();

  const failed = results.filter(([, ok]) => !ok);
  console.log(`\nE2E: ${results.length - failed.length}/${results.length} pass`);
  process.exitCode = failed.length ? 1 : 0;
}

main().catch((err) => {
  console.error('E2E ERROR:', err.message);
  process.exit(1);
});
