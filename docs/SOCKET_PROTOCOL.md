# Socket Protocol

Client → Server: CREATE_ROOM {playerName}, JOIN_ROOM {roomCode, playerName},
SELECT_DECK {heroId, roomCode}, PLAY_CARD {gameId, cardInstanceId, targetId?},
ATTACK {gameId, attackerId, targetId}, END_TURN {gameId},
USE_HERO_POWER {gameId}, DRAW_CARD {gameId}, CONCEDE {gameId},
RECONNECT_GAME {sessionToken}, REMATCH {gameId}.
DRAW_CARD rút 1 lá mỗi turn (tay tối đa 6, deck hết hoặc tay đầy bị reject).

Payload được runtime-validate: tên tối đa 24 ký tự, room code tối đa 6, các id tối đa 128.
Payload sai trả `ACTION_REJECTED {code: "INVALID_PAYLOAD", message}`.

Server → Client: ROOM_CREATED {roomCode, playerId, sessionToken},
PLAYER_JOINED {roomCode, players, playerId?, sessionToken?}
(token chỉ gửi riêng cho người vừa join),
ROOM_READY {roomCode}, DECK_SELECTED {playerId, heroId},
GAME_STARTED {gameId}, GAME_STATE_UPDATED {gameState},
ACTION_REJECTED {code, message}, TURN_CHANGED {activePlayerId, turn},
GAME_OVER {winnerId}, PLAYER_DISCONNECTED {playerId}.
REMATCH_REQUESTED {playerId, votes, required}; khi đủ 2 vote, server phát
GAME_STARTED với gameId mới.

Auth: sessionToken trong handshake.auth. Reconnect đổi socket.id —
server join lại room theo token, gửi lại state (không reset game).

Bảo mật: hand đối thủ bị che theo từng viewer; mọi rule validate ở server
(GameRuleError → ACTION_REJECTED). Client chỉ gửi ID/action.

Target/combat: spell single-target cần targetId thuộc phe cho phép theo data
(ENEMY_* chỉ phe địch; Curse of Weakness là ENEMY_MINION); lá multi-effect
(Siphon Soul) chỉ effect cần chọn tay mới dùng targetId gửi lên. Attack chỉ
dùng quái của mình đánh sang phe địch, không tự đánh mình; attacker dùng quái
địch sai phe bị reject. Có Taunt sống bên địch thì phải đánh Taunt trước
(code TAUNT_REQUIRED; spell damage bỏ qua Taunt). Các mã reject liên quan:
HAND_FULL, DECK_EMPTY, ALREADY_DREW, TAUNT_REQUIRED, INVALID_TARGET.
