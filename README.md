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

Trên Windows PowerShell, nếu `npm` báo lỗi tìm `AppData\Roaming\npm\node_modules\npm\bin\npm-cli.js`, dùng `npm.cmd` thay cho `npm` trong các lệnh bên dưới. Môi trường hiện đã có Node.js và npm chính thức tại `C:\Program Files\nodejs`.

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

## Cách chơi giữa 2 máy cùng Wi-Fi/LAN

Máy A chạy server và không được tắt trong lúc chơi. Máy B chỉ cần trình duyệt, không cần cài Node.js hay database.

```bash
# Chạy từ thư mục gốc project trên máy A
npm run dev:lan
```

Trên máy A, chạy `ipconfig` và lấy địa chỉ IPv4 của card Wi-Fi hoặc Ethernet, ví dụ `192.168.1.50`. Cả hai máy mở:

```text
http://192.168.1.50:5173
```

Máy A chọn **Tạo phòng**, máy B nhập tên và mã phòng rồi chọn **Tham gia**.

Nếu máy B không mở được trang, trên máy A kiểm tra Windows Firewall và cho phép TCP port 5173 trong mạng **Private**. Có thể tạo rule bằng PowerShell chạy với quyền Administrator:

```powershell
New-NetFirewallRule -DisplayName "CoinCard LAN 5173" -Direction Inbound -Protocol TCP -LocalPort 5173 -Action Allow -Profile Private
```

Không mở port 3000 cho máy khác; Vite proxy Socket.IO tới backend `127.0.0.1:3000` trên máy A. Nếu hai thiết bị dùng Wi-Fi khách hoặc router bật AP/client isolation, chúng không thể kết nối trực tiếp dù cùng tên mạng; hãy chuyển sang mạng Private thông thường.

## Chơi khác mạng bằng link Internet tạm

Để chơi qua Wi-Fi/4G khác nhau, dùng bản production một cổng và Cloudflare Quick Tunnel. Máy chủ vẫn giữ database và game state; máy khách chỉ cần trình duyệt.

```bash
# Lần đầu hoặc sau khi sửa code
npm run build

# Terminal 1: chạy server + giao diện production
npm start

# Terminal 2: tạo link HTTPS tạm
npm run tunnel
```

Gửi link `https://....trycloudflare.com` được in ở Terminal 2 cho người chơi còn lại. Cả hai mở cùng link đó. Không mở cổng 3000/5173 trên router; máy chủ phải giữ cả hai terminal hoạt động. Link Quick Tunnel là link tạm và thường đổi sau khi chạy lại tunnel.

`npm start` cần file `server/.env` được tạo từ `server/.env.example`, database đã migrate/seed và client đã build. `cloudflared` phải được cài riêng trên máy chủ và có trong PATH.

Luật chi tiết: `docs/GAME_RULES.md`.

## Scripts hay dùng
```bash
npm run dev          # chạy server + client
npm run build        # build shared + server + client production
npm start            # chạy production server tại cổng 3000
npm run tunnel       # tạo link HTTPS tạm tới localhost:3000
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
