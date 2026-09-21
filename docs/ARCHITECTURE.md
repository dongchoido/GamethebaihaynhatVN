# Kiến trúc và OOP

CoinCard gồm Spring Boot backend authoritative và React browser client. Hai phía
giao tiếp qua WebSocket thuần tại `/ws`; client không tự tính kết quả action.

## Backend Java (Java là trung tâm)

- `model/GameCharacter.java` (abstract) ← `Hero` / `Minion` : Inheritance, `takeDamage/heal/isDead` chung.
- `game/`: `Game`, `Player`, `Deck` và `GameEngine` (validate → checkpoint → commit, Memento).
- `game/effects/`: `ICardEffect` (EffectStrategy) + 10 strategy `Damage/Heal/Buff/Transform/...` : Strategy + Polymorphism.
- `game/HeroPower.java`: registry Strategy cho 5 hero power.
- `combat/CombatService.java`: luật Charge, Taunt, minion chắn hero và damage đồng thời.
- `mapper/GameStateMapper.java`: tập trung serialize `Hero/Minion/Card/Player/Game/Room` — SRP, tách khỏi domain.
- `service/GameService` (gameplay) / `RoomService` (phòng) / `GamePersistenceService` (lưu DB) : SRP.
- `room/`: phòng hai người, reconnect token, rematch vote và index O(1).
- `net/GameService`: xác thực session/socket/game, khóa `synchronized(game)` mutation, snapshot riêng viewer.
- `ws/GameWebSocketHandler`: validate envelope `{event, data}` và route action, không chứa business logic.
- `db/`: `CatalogRepository`/`GameRepository` (abstraction) → `JdbcCatalogRepository`/`JdbcGameRepository` : Repository + DIP, JDBC giữ nguyên.

Action trong `GameEngine` đi theo thứ tự validate -> checkpoint -> commit. Nếu
effect sau thất bại, checkpoint phục hồi hand, mana, board, deck, hero và thống kê.
Collection/domain state không được sửa trực tiếp từ network layer.

## Frontend

- `CompatSocket` quản lý WebSocket và reconnect trong mỗi tab.
- `socketService` là cổng duy nhất để component gửi action.
- `gameStore` giữ session trong `sessionStorage`; mỗi tab có session riêng.
- UI chuyển màn dựa trên snapshot `PLAYING`/`FINISHED` từ server.
- Hand đối thủ không được gửi trong snapshot, thay vì chỉ ẩn bằng CSS.

## UML

```mermaid
classDiagram
  GameCharacter <|-- Hero
  GameCharacter <|-- Minion
  GameCharacter : -currentHealth
  GameCharacter : -maxHealth
  GameCharacter : +takeDamage()
  GameCharacter : +heal()
  GameCharacter : +isDead()
  GameCharacter : +getCharacterType()
```

```mermaid
classDiagram
  class ICardEffect {
    <<interface>>
    +execute(EffectContext)
  }
  ICardEffect <|.. DamageEffect
  ICardEffect <|.. HealEffect
  ICardEffect <|.. DestroyEffect
  ICardEffect <|.. TransformEffect
  ICardEffect <|.. BuffAttackEffect
  ICardEffect <|.. BuffHealthEffect
  ICardEffect <|.. AoeDamageEffect
  EffectResolver --> ICardEffect : Strategy
```

```mermaid
classDiagram
  class HeroPower {
    <<interface>>
    +validate(Game, Player)
    +execute(Game, Player)
  }
  HeroPower <|.. DamagePower
  HeroPower <|.. RecruitPower
  HeroPower <|.. HealPower
  HeroPower <|.. DrawPower
```

```mermaid
classDiagram
  GameRepository <|.. JdbcGameRepository
  CatalogRepository <|.. JdbcCatalogRepository
  GamePersistenceService --> GameRepository
```

```mermaid
classDiagram
  GameWebSocketHandler --> GameService
  GameService --> RoomManager
  GameService --> GamePersistenceService
  GameService --> RoomService
  RoomManager "1" *-- "*" Room
  GameService --> GameEngine
  GameService --> CatalogRepository
  GameService --> GameRepository
  CatalogRepository <|.. JdbcCatalogRepository
  GameRepository <|.. JdbcGameRepository
  GameEngine "1" *-- "*" Game
  Game "1" *-- "2" Player
  Player *-- Hero
  Player *-- Deck
  Player *-- Minion
  GameEngine --> EffectResolver
  EffectResolver --> ICardEffect
  GameEngine --> HeroPower
  GameService --> GameStateMapper
```

```mermaid
sequenceDiagram
  participant C as React clients
  participant W as WebSocket handler
  participant S as GameService
  participant E as GameEngine
  participant D as JDBC repository
  C->>W: CREATE/JOIN + SELECT_DECK
  W->>S: validated command
  S->>E: create/start game
  S-->>C: viewer-specific snapshot
  C->>W: action + gameId
  W->>S: validated command
  S->>E: validate/commit under game lock
  S-->>C: viewer-specific snapshot
  S->>D: transaction when FINISHED
```

Game/phòng đang chơi nằm trong RAM. Phòng cả hai offline quá thời hạn sẽ được dọn.
Restart server không phục hồi trận đang chơi; kết quả đã kết thúc nằm trong SQLite.
