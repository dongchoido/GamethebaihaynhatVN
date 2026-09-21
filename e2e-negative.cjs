// Negative tests: lỗi client không được giết server.
const { connect } = require('./e2e-ws.cjs');

const URL = process.env.GAME_URL || 'http://localhost:3000';
let pass = 0;
let fail = 0;
const check = (name, ok) => {
  if (ok) pass++;
  else fail++;
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
  // 1. Join mã phòng không tồn tại → REJECT, server sống
  const bad = connect(URL);
  await once(bad, 'connect');
  const rejP = once(bad, 'ACTION_REJECTED');
  bad.emit('JOIN_ROOM', { roomCode: 'ZZZZZZ', playerName: 'Ghost' });
  const rej = await rejP;
  check('sai mã phòng → ROOM_NOT_FOUND reject', rej.code === 'ROOM_NOT_FOUND');

  // 2. Tạo phòng thật, nhồi người thứ 3 → ROOM_FULL reject
  const p1 = connect(URL);
  const p2 = connect(URL);
  const p3 = connect(URL);
  await Promise.all([once(p1, 'connect'), once(p2, 'connect'), once(p3, 'connect')]);
  const createdP = once(p1, 'ROOM_CREATED');
  p1.emit('CREATE_ROOM', { playerName: 'A' });
  const created = await createdP;
  const joinedP = once(p2, 'PLAYER_JOINED');
  p2.emit('JOIN_ROOM', { roomCode: created.roomCode, playerName: 'B' });
  await joinedP;
  const fullP = once(p3, 'ACTION_REJECTED');
  p3.emit('JOIN_ROOM', { roomCode: created.roomCode, playerName: 'C' });
  const full = await fullP;
  check('người thứ 3 → ROOM_FULL reject', full.code === 'ROOM_FULL');

  // 3. Action với gameId vớ vẩn → reject, server sống
  const badGameP = once(p1, 'ACTION_REJECTED');
  p1.emit('PLAY_CARD', { gameId: 'nope', cardInstanceId: 'x' });
  await badGameP;
  check('gameId sai → reject không crash', true);

  // 4. Server còn sống: tạo phòng mới được
  const p4 = connect(URL);
  await once(p4, 'connect');
  const okP = once(p4, 'ROOM_CREATED');
  p4.emit('CREATE_ROOM', { playerName: 'Alive' });
  const okRoom = await okP;
  check('server còn sống sau loạt lỗi', !!okRoom.roomCode);

  for (const s of [bad, p1, p2, p3, p4]) s.disconnect();
  console.log(`\nNEGATIVE: ${pass}/${pass + fail} pass`);
  process.exitCode = fail ? 1 : 0;
}

main().catch((err) => {
  console.error('NEGATIVE ERROR:', err.message);
  process.exit(1);
});
