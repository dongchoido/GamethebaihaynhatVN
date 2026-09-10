# CoinCard — Game thẻ bài online 2 người chơi

Bài tập lớn môn OOP (nhóm D24CN09-B PTIT). Gameplay lấy cảm hứng Hearthstone,
toàn bộ code/database/cấu trúc tự xây dựng. Server là authoritative:
client chỉ gửi action, mọi damage/mana/draw/HP do server quyết.

## Tech stack
- Client: React + TypeScript + Vite (`client/`, port 5173)
- Server: Node.js + Express + Socket.IO (`server/`, port 3000)
- DB: SQLite + Prisma (`server/prisma/`, file `data/coincard.db`)
- Shared types/events: `shared/` (cả 2 bên cùng dùng)

## Yêu cầu
- Node.js >= 18 (đã test với Node 24)
- npm >= 9

## Cách chạy game (lần đầu)
```bash
# 1. Cài dependencies (từ thư mục gốc project)
npm install

# 2. Build shared types
npm run build:shared

# 3. Tạo file .env cho server
cd server
copy .env.example .env      # Windows
# cp .env.example .env      # macOS/Linux
cd ..

# 4. Tạo database + seed 34 lá bài + 5 hero
npm run prisma:migrate --workspace server
npm run seed

# 5. Chạy cả server + client
npm run dev
```

## Cách chơi (2 người trên localhost)
1. Mở **2 tab** trình duyệt vào `http://localhost:5173`
2. Tab 1: nhập tên → **Tạo phòng** → được mã phòng (vd `A8F3K2`) → **chọn hero**
3. Tab 2: nhập tên + mã phòng → **Tham gia** → **chọn hero**
4. Đủ 2 người chọn hero → trận đấu bắt đầu, đánh theo lượt:
   - Click lá bài để đánh (spell cần target thì click tiếp vào mục tiêu)
   - Click minion của mình rồi click mục tiêu để tấn công
   - Nút hero power, nút END TURN, Đầu hàng
5. Hero nào hết HP trước thì thua. Kết quả lưu vào database.

Luật chi tiết: `docs/GAME_RULES.md`.

## Scripts hay dùng
```bash
npm run dev          # chạy server + client
npm test             # unit test GameEngine
npm run test:e2e     # test 2 người chơi qua socket thật (cần server chạy)
npm run test:e2e-neg # test các case lỗi (sai mã phòng, phòng đầy...)
npm run build        # build shared + server
```

Trong `client/`: `npm run dev` (chỉ client), `npm run build`, `npx tsc --noEmit -p tsconfig.json` (check type).
Trong `server/`: `npm run dev` (chỉ server), `npm run seed` (seed lại bài).

## Docs
- `docs/ARCHITECTURE.md` — kiến trúc client/server/shared
- `docs/ASSET_MAPPING.md` — mapping ảnh → component gameplay
- `docs/GAME_RULES.md` — luật chơi
- `docs/SOCKET_PROTOCOL.md` — giao thức socket
- `docs/DATABASE.md` — schema + seed

Luồng code để học: click lá bài → `GameScreen` → `socketService` →
`SocketHandler` → `GameService` → `GameEngine` → domain
(`Game`/`Player`/`Hero`/`Minion`) → broadcast `GameState` → UI render.
