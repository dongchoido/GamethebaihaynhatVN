# Database (SQLite + Prisma)

Schema: `server/prisma/schema.prisma`. SQLite không có enum → dùng String
+ validate ở app (shared types).

## Bảng
- Card: id, name, slug (unique), description, type, rarity, manaCost,
  attack, health, heroClass, imagePath (chỉ lưu path, không lưu binary),
  effects (Json), keywords (Json), collectible, cardSet.
- Hero: id, name, heroClass (unique), powerName, powerCost, imagePath.
- Deck / DeckCard: deck 30 lá của player.
- Player: id, name.
- Game: id, roomCode (unique), status, startedAt, finishedAt, winnerId.
- GamePlayer: gameId + playerId (unique), deckId?, winner.
- GameHistory / GameAction: log action theo turn.

## Seed
`npm run seed --workspace server` (tsx src/database/seed.ts):
đọc `data/cards.json` (soạn từ tên file ảnh trong
`client/public/assets/images/Minions/`) → upsert Card + 5 Hero.
Số liệu attack/health/mana theo đúng thẻ Hearthstone thật.

## Lịch sử trận
Khi game FINISHED hoặc CONCEDE, GameService lưu Game + GamePlayer qua
IGameRepository (PrismaGameRepository, inject qua constructor).
