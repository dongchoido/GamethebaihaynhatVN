# CoinCard - Game thẻ bài online 2 người chơi

Bài tập lớn môn OOP (nhóm D24CN09-B PTIT). Server là authoritative: client chỉ
gửi action, còn luật, mana, damage, rút bài và kết quả do Java backend quyết định.

## Công nghệ

- Backend: Java 17, Spring Boot 3, WebSocket, JDBC và SQLite (`server-java/`).
- Frontend: React 19, TypeScript và Vite (`client/`).
- DTO/event phía trình duyệt: TypeScript (`shared/`).
- Dữ liệu bài: `data/cards.json`; database runtime: `data/coincard.db`.

Không còn backend Node, Socket.IO hoặc Prisma. TypeScript chỉ phục vụ giao diện
trình duyệt và kiểu dữ liệu client.

## Yêu cầu

- Java 17 trở lên.
- Node.js 18 trở lên và npm 9 trở lên.
- Không cần cài Maven; project có Maven Wrapper trong `server-java/`.

## Chạy development

Từ thư mục gốc project:

```bash
npm install
npm run dev
```

Mở `http://localhost:5173`. Lệnh này chạy Spring Boot tại cổng 3000 và Vite tại
cổng 5173; Vite proxy `/ws` tới Java backend.

## Chạy production

```bash
npm install
npm run build
npm start
```

Mở `http://localhost:3000`. Spring Boot phục vụ cả React build, `/health` và
WebSocket `/ws`. Schema SQLite và catalog 34 lá/5 hero được đồng bộ khi khởi động.

Các đường dẫn có thể đổi bằng biến môi trường:

- `PORT`, `HOST`
- `COINCARD_DB_PATH`
- `COINCARD_CARDS_PATH`
- `COINCARD_CLIENT_DIST`

## Chơi hai người

1. Người thứ nhất nhập tên, tạo phòng và chọn hero.
2. Người thứ hai nhập tên, mã phòng rồi chọn hero.
3. Đủ hai người chọn hero thì trận tự bắt đầu.
4. Click bài để chơi; spell cần mục tiêu thì click mục tiêu tiếp theo.
5. Click minion của mình rồi click minion/hero địch để tấn công.
6. Có thể rút thêm một lá mỗi lượt, dùng hero power, kết thúc lượt hoặc đầu hàng.

Luật đầy đủ nằm tại `docs/GAME_RULES.md`.

## LAN và Internet

Build production trước, sau đó trên PowerShell:

```powershell
$env:HOST="0.0.0.0"
npm start
```

Hai máy cùng mạng mở `http://<IPv4-máy-chủ>:3000`. Nếu cần link Internet tạm,
giữ server chạy và mở terminal khác:

```bash
npm run tunnel
```

## Kiểm thử

```bash
npm run verify               # typecheck + 47 JUnit + build + production E2E
npm run test                 # Java unit/regression/database tests
npm run test:e2e             # 2 người chơi; cần Java server đang chạy
npm run test:e2e-neg         # payload/action lỗi
npm run test:e2e-connection  # reconnect và tiếp tục trận
npm run test:e2e-tabs        # cần Java server + Vite dev
```

## OOP — trả lời khi bảo vệ

### Encapsulation
State `private` và chỉ đổi qua behavior: `Player.spendMana()`, `Hero.takeDamage()/heal()`, `Minion.takeDamage()/modifyAttack()`, `Deck.drawOne()`, `Game.switchTurn()`.
Collection không expose mutable: `Player.getBoard()` → `unmodifiableList`, `handCards()` → `unmodifiableList`, `Game.getPlayers()` → `copy`.
→ File: `server-java/src/main/java/vn/coincard/server/game/Player.java:31`, `model/GameCharacter.java:10`, `game/Deck.java:18`

### Inheritance
`GameCharacter` (abstract) → `Hero` và `Minion` kế thừa chung `takeDamage/heal/isDead/checkpoint`.
→ File: `server-java/src/main/java/vn/coincard/server/model/GameCharacter.java:7`, `game/Hero.java:6`, `game/Minion.java:6`

### Abstraction
`EffectStrategy` (`ICardEffect`), `HeroPower`, `GameRepository/CatalogRepository`, `MessageSender`.
→ File: `game/effects/ICardEffect.java:3`, `game/HeroPower.java:7`, `db/Repositories.java:8`, `net/MessageSender.java:3`

### Polymorphism
`GameEngine` gọi `strategy.validate/ execute` không biết cụ thể `DamageEffect/HealEffect/DestroyEffect/TransformEffect/...`
→ File: `game/EffectResolver.java:20`, `game/effects/*`, `game/HeroPower.java:17`

### SOLID
- SRP: `GameService` (gameplay) / `RoomService` (phòng) / `GamePersistenceService` (lưu DB) / `GameStateMapper` (serialize) → `server-java/src/main/java/vn/coincard/server/service/*`, `mapper/GameStateMapper.java:12`
- OCP: thêm effect mới chỉ thêm class `EffectStrategy` không sửa resolver
- LSP: `Hero`/`Minion` dùng như `GameCharacter` trong `GameCharacterTest`
- ISP: interface nhỏ (`EffectStrategy`, `HeroPower`, `GameRepository`)
- DIP: `GameService` phụ thuộc `GameRepository` abstraction, không phụ thuộc JDBC

### Design Patterns thực sự dùng
- Strategy: `EffectStrategy`, `HeroPower`
- Repository: `GameRepository`/`CatalogRepository` → `Jdbc*`
- Memento: `checkpoint()` trả `Runnable` undo trong `Player/Hero/Minion/Deck` → `GameEngine.playCard:43`
- Command: `GameAction` functional interface trong `GameService:234`
- Factory: `Deck` shuffle, `TokenCards` tạo token

## Cấu trúc

```text
server-java/  Spring Boot, domain game, WebSocket, room, JDBC và JUnit (Java chính)
  src/main/java/vn/coincard/server/
    model/GameCharacter.java      # Inheritance
    game/Hero.java, Minion.java   # Domain
    effect/*, power/*             # Strategy
    combat/CombatService.java
    service/GameService, RoomService, GamePersistenceService  # SRP
    mapper/GameStateMapper.java   # Serialization tách biệt
    repository/*, db/*            # Repository
    websocket/*, net/*            # WebSocket
client/       React UI, WebSocket client và assets (chỉ frontend)
shared/       event, payload và state type dùng bởi React
data/         card catalog; file SQLite runtime bị gitignore
docs/         kiến trúc, database, protocol, luật và báo cáo test
```

Luồng action: `GameScreen` -> `socketService` -> `/ws` ->
`GameWebSocketHandler` -> `GameService` -> `GameEngine` -> domain (`Game/Player/Hero/Minion` là `GameCharacter`) -> `GameStateMapper` -> snapshot riêng
cho từng người chơi.

## Build Java độc lập

```bash
# Windows
.\server-java\mvnw.cmd clean test
.\server-java\mvnw.cmd package
# Linux/macOS
./server-java/mvnw clean test
./server-java/mvnw package
# NPM wrapper (cross-platform)
npm run test:java
npm run build:java
```
