# CoinCard

CoinCard là bài tập lớn OOP hai người chơi. Java 17/Spring Boot là authoritative
game core: client chỉ hiển thị snapshot và gửi command; Java quyết định deck,
mana, effect, combat, turn, fatigue và kết quả.

## Công nghệ

- Backend: Java 17, Spring Boot 3.5, WebSocket, JDBC, SQLite (`server-java/`).
- Frontend: React 19, TypeScript, Vite (`client/`).
- Shared types: TypeScript (`shared/`).
- Catalog nguồn: `data/cards.json`; database runtime: SQLite.

## Chạy

Yêu cầu Java 17+ và Node.js 18+.

```bash
npm install
npm run dev
```

Mở `http://localhost:5173`. Production:

```bash
npm install
npm run build
npm start
```

Mở `http://localhost:3000`. Các biến môi trường hỗ trợ: `PORT`, `HOST`,
`COINCARD_DB_PATH`, `COINCARD_CARDS_PATH`, `COINCARD_CLIENT_DIST`.

## Chia sẻ demo qua Cloudflare

Sau khi production server đã chạy, mở terminal khác:

```bash
npm run tunnel
```

Cloudflare in URL tạm thời trên terminal. URL dừng hoạt động khi tunnel process dừng.

## Luồng chơi

1. Hai người tạo hoặc vào phòng, chọn hero và mở `DeckBuilderScreen`.
2. Deck phải có đúng 30 lá collectible, chỉ neutral hoặc đúng hero class, tối đa
   2 bản thường và 1 Legendary. Java luôn validate lại payload.
3. Người đi trước nhận 3 lá rồi tự rút ở lượt đầu; người đi sau nhận 4 lá và
   The Coin. Không có mulligan hay rút thủ công.
4. Tay tối đa 10 lá; rút khi đầy sẽ burn. Deck hết gây fatigue tăng dần 1, 2, 3...
   Hero power dùng tối đa một lần mỗi lượt.

Catalog dành cho deck builder: `GET /api/game-catalog`. Protocol WebSocket dùng
`SUBMIT_LOADOUT`; các event cũ `SELECT_DECK` và `DRAW_CARD` đã bị loại bỏ.

## Kiểm thử và quality gate

```bash
npm run verify
npm run test:java
npm run test:e2e
npm run test:e2e-rules
npm run test:e2e-neg
npm run test:e2e-connection
npm run test:e2e-tabs
```

`verify` chạy shared build, TypeScript typecheck, frontend tests, Maven verify,
production build và E2E trên server production. E2E chạy catalog CoinCard thật
cho luồng người chơi và catalog fixture riêng cho các luật cần thứ tự rút xác định.
Các E2E này là Node WebSocket clients kết nối vào Spring Boot production JAR;
component test bao phủ UI, còn browser UI automation không nằm trong quality gate.
Maven Wrapper binary nằm trong `server-java/`;
Windows dùng `server-java\mvnw.cmd`, Linux/macOS dùng `server-java/mvnw`.

## OOP trong production

- Encapsulation: `Game`/`Player` kiểm soát mutation; collection trả immutable view.
- Inheritance: `GameCharacter` abstract được `Hero` và `Minion` mở rộng.
- Strategy: `CardEffect`, `EffectStrategy` registry và `HeroPower`.
- Command: sealed `GameCommand` được dispatch duy nhất bởi `GameCommandHandler`.
- Factory: `DeckFactory` tạo deck hợp lệ, `GameFactory` là cổng production duy nhất
  để tạo `Game` aggregate.
- Memento: `GameMemento` rollback player/deck/character và turn/status state khi command nhiều effect lỗi.
- Repository: `CatalogRepository`, `GameResultRepository` tách khỏi JDBC adapter.
- Concurrency: `GameCommandHandler` lấy `GameSession` và khóa riêng từng game;
  `GameEngine` là domain service stateless, test không tự khóa.

## Cấu trúc

```text
server-java/src/main/java/vn/coincard/server/
  game/       aggregate, command, deck rules, effects, combat
  application/ loadout, match, command execution, cleanup services
  room/       room lifecycle, reconnect, rematch
  net/        WebSocket-facing facade and outbound port
  db/         repository ports và JDBC adapter
  mapper/     viewer-specific state mapping
  ws/         WebSocket transport và session registry
client/       React UI, DeckBuilderScreen, socket client
shared/       TypeScript state/event/payload contracts
data/         card catalog
docs/         rules, protocol, architecture, database, test report
```

Chi tiết: [docs/GAME_RULES.md](docs/GAME_RULES.md),
[docs/SOCKET_PROTOCOL.md](docs/SOCKET_PROTOCOL.md),
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
