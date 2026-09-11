# Socket protocol

Socket.IO cùng origin trang. Dev proxy /socket.io qua Vite; production Express dùng chung cổng frontend/socket. Mỗi tab lưu token riêng trong sessionStorage.

| Client event | Payload |
| --- | --- |
| CREATE_ROOM | playerName |
| JOIN_ROOM | roomCode, playerName |
| SELECT_DECK | roomCode, heroId (MAGE/HUNTER/PALADIN/PRIEST/WARLOCK) |
| PLAY_CARD | gameId, cardInstanceId, targetId? |
| ATTACK | gameId, attackerId, targetId |
| END_TURN / DRAW_CARD / USE_HERO_POWER / CONCEDE / REMATCH | gameId |
| RECONNECT_GAME | sessionToken |

| Server event | Nội dung |
| --- | --- |
| ROOM_CREATED | roomCode, playerId, sessionToken, players |
| PLAYER_JOINED | roomCode, players; token/id chỉ gửi riêng người mới join |
| ROOM_READY | roomCode |
| DECK_SELECTED | playerId, heroId |
| GAME_STARTED | gameId; đợi snapshot để chuyển màn |
| GAME_STATE_UPDATED | gameState theo viewer |
| TURN_CHANGED | activePlayerId, turn; chỉ khi turn đổi |
| GAME_OVER | winnerId; null là hòa |
| PLAYER_DISCONNECTED | playerId |
| REMATCH_REQUESTED | playerId, votes, required |
| ACTION_REJECTED | code, message |

Room players gồm playerId/name/heroClass/ready/connected; không lộ token. GameState gồm gameId/roomCode/status/turn/activePlayerId/players/winnerId/statusMessage/manualDrawUsed. PlayerState chứa hero/board/hand/handCount/deckCount/mana/maxMana/damageDealt/cardsPlayed/minionsSummoned. Hand đối thủ luôn [].

Reconnect trả room snapshot và game snapshot nếu có. Socket mới chiếm session hợp lệ, socket cũ bị ngắt. Action phải khớp token, socketId hiện tại và gameId hiện tại. Game cũ bị xóa sau rematch. PLAYING → game; FINISHED → over kể cả refresh; RECONNECT_FAILED xóa session/auth. Mất mạng khóa thao tác, socket tự nối lại. Đổi URL tunnel tạo origin mới nên bắt đầu phòng mới.

Runtime validation: tên tối đa 24 ký tự, roomCode tối đa 6, gameId 64, card/target/token 128. Lỗi gồm INVALID_PAYLOAD, INVALID_TARGET, HAND_FULL, DECK_EMPTY, ALREADY_DREW, TAUNT_REQUIRED. Không coi action thành công trước snapshot.

