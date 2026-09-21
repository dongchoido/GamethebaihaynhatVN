# WebSocket protocol

Endpoint: `/ws`. Mỗi text frame là JSON:

```json
{ "event": "EVENT_NAME", "data": {} }
```

Vite proxy `/ws` về Spring Boot khi development. Production dùng cùng origin và
cùng cổng 3000. Mỗi tab lưu reconnect token riêng trong `sessionStorage`.

| Client event | Payload |
| --- | --- |
| `CREATE_ROOM` | `playerName` |
| `JOIN_ROOM` | `roomCode`, `playerName` |
| `SELECT_DECK` | `roomCode`, `heroId` |
| `PLAY_CARD` | `gameId`, `cardInstanceId`, `targetId?` |
| `ATTACK` | `gameId`, `attackerId`, `targetId` |
| `END_TURN`, `DRAW_CARD`, `USE_HERO_POWER`, `CONCEDE`, `REMATCH` | `gameId` |
| `RECONNECT_GAME` | `sessionToken` |

| Server event | Nội dung |
| --- | --- |
| `ROOM_CREATED` | `roomCode`, `playerId`, `sessionToken`, `players` |
| `PLAYER_JOINED` | `roomCode`, `players`; token/id chỉ gửi riêng người vừa join |
| `ROOM_READY` | `roomCode` |
| `GAME_STARTED` | `gameId` |
| `GAME_STATE_UPDATED` | `gameState` riêng theo viewer |
| `GAME_OVER` | `winnerId`; null nếu hòa |
| `PLAYER_DISCONNECTED` | `playerId` |
| `ACTION_REJECTED` | `code`, `message` |

Room snapshot không chứa token. Hand đối thủ luôn là mảng rỗng. Reconnect token
hợp lệ được bind vào socket mới và socket cũ bị đóng. Action phải khớp token,
socket hiện tại và game hiện tại của room.

Giới hạn validation: tên 24 ký tự, room code 6, game ID 64, card/target/token 128.
Các lỗi luật gồm `INVALID_PAYLOAD`, `INVALID_TARGET`, `HAND_FULL`, `DECK_EMPTY`,
`ALREADY_DREW`, `TAUNT_REQUIRED` và `MINIONS_BLOCK_HERO`.
