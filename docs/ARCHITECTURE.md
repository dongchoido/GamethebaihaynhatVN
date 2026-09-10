# Kiến trúc

Monorepo npm workspaces: `client` (React+Vite), `server` (Express+Socket.IO), `shared` (types/events chung), `data/cards.json`, `prisma` (schema+seed, trong server).

## Nguyên tắc
- Server AUTHORITATIVE. Client gửi action, server validate → GameEngine → broadcast.
- SOLID: RoomManager, GameEngine, EffectResolver, PrismaGameRepository (inject).
- OOP: Minion/Hero/Player/Game class; mọi damage qua method (takeDamage), check logic tại object.
- No magic numbers: các hằng ở `server/src/game/constants.ts`.

## Cấu trúc server
```
server/src/
  config.ts
  index.ts (bootstrap)
  room/Room.ts RoomManager.ts
  game/Game.ts Player.ts Hero.ts Minion.ts Deck.ts
  game/effects/{DamageEffect,HealEffect,DrawEffect,BuffAttackEffect,BuffHealthEffect,SummonEffect,AoeDamageEffect}.ts
  game/GameEngine.ts EffectResolver.ts
  database/CardRepository.ts GameRepository.ts
  network/{SocketHandler,GameService}.ts
  data/cards.ts (load cards.json)
  prisma/schema.prisma + seed.ts
```

## Client
```
client/src/
  assets/assetRegistry.ts (duy nhất 1 nơi biết path)
  socket/socketService.ts socketStore.ts (one place socket)
  screens/{Home,Lobby,Game,GameOver}.tsx
  components/common/{Card,Badge}.tsx components/game/{...}
```

## Shared
```
shared/src/types/{cards,game,socketPayloads}.ts
shared/src/events/socketEvents.ts
```

Cả client và server import `@coincard/shared`.
