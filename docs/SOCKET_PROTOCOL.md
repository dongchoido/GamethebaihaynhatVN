# Socket Protocol

Client → Server: CREATE_ROOM {playerName}, JOIN_ROOM {roomCode, playerName},
SELECT_DECK {heroId, roomCode}, PLAY_CARD {gameId, cardInstanceId, targetId?},
ATTACK {gameId, attackerId, targetId}, END_TURN {gameId},
USE_HERO_POWER {gameId}, CONCEDE {gameId}, RECONNECT_GAME {sessionToken}.

Server → Client: ROOM_CREATED {roomCode, playerId, sessionToken},
PLAYER_JOINED {roomCode, players, playerId?, sessionToken?}
(token chỉ gửi riêng cho người vừa join),
ROOM_READY {roomCode}, DECK_SELECTED {playerId, heroId},
GAME_STARTED {gameId}, GAME_STATE_UPDATED {gameState},
ACTION_REJECTED {code, message}, TURN_CHANGED {activePlayerId, turn},
GAME_OVER {winnerId}, PLAYER_DISCONNECTED {playerId}.

Auth: sessionToken trong handshake.auth. Reconnect đổi socket.id —
server join lại room theo token, gửi lại state (không reset game).

Bảo mật: hand đối thủ bị che theo từng viewer; mọi rule validate ở server
(GameRuleError → ACTION_REJECTED). Client chỉ gửi ID/action.
