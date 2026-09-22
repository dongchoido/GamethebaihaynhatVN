# WebSocket Protocol

Endpoint: `/ws`. Mỗi text frame là JSON:

```json
{ "event": "EVENT_NAME", "data": {} }
```

Vite proxy `/ws` về Spring Boot khi development; production dùng cùng origin.
Mỗi tab giữ reconnect token riêng trong `sessionStorage`.

## Client events

| Event | Payload |
| --- | --- |
| `CREATE_ROOM` | `playerName` |
| `JOIN_ROOM` | `roomCode`, `playerName` |
| `SUBMIT_LOADOUT` | `roomCode`, `heroClass`, `cardSlugs[30]` |
| `PLAY_CARD` | `gameId`, `cardInstanceId`, `targetId?` |
| `ATTACK` | `gameId`, `attackerId`, `targetId` |
| `END_TURN`, `USE_HERO_POWER`, `CONCEDE`, `REMATCH` | `gameId` |
| `RECONNECT_GAME` | `sessionToken` |

`SELECT_DECK` và `DRAW_CARD` không còn thuộc protocol.

## HTTP catalog

`GET /api/game-catalog` trả:

```json
{
  "heroes": [],
  "collectibleCards": [],
  "deckRules": {
    "deckSize": 30,
    "maxCopies": 2,
    "maxLegendaryCopies": 1,
    "heroClasses": ["MAGE", "HUNTER", "PALADIN", "PRIEST", "WARLOCK"]
  },
  "suggestedDecks": { "MAGE": ["card-slug", "..."] }
}
```

Token card không xuất hiện trong `collectibleCards` hoặc suggested deck.

## Server events

| Event | Nội dung |
| --- | --- |
| `ROOM_CREATED` | `roomCode`, `playerId`, `sessionToken`, `players` |
| `PLAYER_JOINED` | `roomCode`, `players` |
| `LOADOUT_ACCEPTED` | `roomCode`, `players` với `heroClass`, `deckReady`, `connected` |
| `GAME_STARTED` | `gameId` |
| `GAME_STATE_UPDATED` | `gameState` riêng theo viewer |
| `GAME_OVER` | `winnerId`; null nếu hòa |
| `PLAYER_DISCONNECTED` | `playerId` |
| `ACTION_REJECTED` | `code`, `message` |

Room snapshot không chứa token hoặc deck đối thủ. Game snapshot có
`heroPowerUsed` và `fatigueDamage` cho từng player; hand đối thủ luôn rỗng.

## Validation và lỗi

Server xác thực room, session ownership, game ownership, payload và deck. Các mã
lỗi luật/chính của protocol mới gồm `INVALID_DECK`, `HERO_POWER_ALREADY_USED`,
`INVALID_COMMAND`, `INVALID_TARGET`, `NOT_PLAYER_TURN`, `NOT_ENOUGH_MANA`,
`TAUNT_REQUIRED`, `BOARD_FULL` và `CARD_NOT_IN_HAND`.

Event không tồn tại hoặc event cũ như `DRAW_CARD` trả `INVALID_COMMAND`. Event hợp lệ
nhưng JSON/field sai trả `INVALID_PAYLOAD`; lỗi nội bộ được log phía server và chỉ trả
`INTERNAL_ERROR`, không gửi exception thô về client.

Giới hạn input: tên 24 ký tự, room code 6, game ID 64, card/target/token 128.
