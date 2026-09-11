# Review và kế hoạch tối ưu CoinCard

## Trạng thái triển khai

Đợt A và các phần nền tảng của Đợt B đã được triển khai:

- Engine validate toàn bộ điều kiện trước mutation; action bị reject không đổi mana, max mana, hand hoặc board.
- Spell thiếu/sai target và Paladin power khi board đầy bị reject nguyên tử.
- `finish()`/`concede()` idempotent; kết quả và persistence không bị ghi đè/lặp.
- Khởi tạo game async có `tryStart/cancelStart`, lỗi không khóa phòng.
- Snapshot `FINISHED` điều hướng về Results; Lobby xử lý reconnect/disconnect.
- `maxHealth` của minion được lưu riêng, không còn bằng health hiện tại.
- Hero của chính mình có thể được chọn làm target khi effect cho phép.
- Room cleanup TTL, `TURN_CHANGED` chỉ phát khi turn thật sự đổi, payload socket có runtime validation.
- Regression suite: 14/14 engine tests, E2E 2 player 16/16, negative 4/4, proxy-tabs 6/6.

Các phần chưa hoàn tất của plan: LEAVE_ROOM đầy đủ, reducer SessionController,
profile React bằng số đo, asset/audio compression, và LAN release đóng gói Express static.

Ngày review: 11/09/2026. Phạm vi: client React, socket, server game engine, vòng đời phòng và tài nguyên. Đây là kế hoạch triển khai; chưa thay đổi gameplay.

## 1. Kết quả kiểm tra

- 10/10 unit test GameEngine đạt.
- TypeScript client và server đạt (`tsc --noEmit`).
- Build production client đạt: JS 284,30 kB (gzip 88,15 kB), CSS 7,16 kB (gzip 1,94 kB).
- Lệnh npm trên máy đang trỏ tới npm-cli.js không tồn tại. Đã chạy trực tiếp CLI trong node_modules để kiểm tra, không sửa cấu hình máy.
- Chưa chạy E2E hai trình duyệt, chưa đo FPS, thời gian render hay độ trễ mạng. Nhận xét responsive dựa trên CSS, chưa phải kết quả kiểm tra hình ảnh thực tế.
- Assets public có 126 file, tổng 94.704.912 byte (~90,3 MiB). Đây là dung lượng thư mục, không phải lượng tải ban đầu: nhiều WAV chưa được gọi ở luồng hiện tại.

## 2. Phát hiện theo mức ưu tiên

### P1 — sửa trước khi thêm tính năng

1. **Bàn đầy làm tăng mana khi action bị từ chối.** `server/src/game/GameEngine.ts:57–71`: lấy bài và trừ mana trước kiểm tra board; rollback gọi increaseMaxMana/refillMana. Người chơi có thể tận dụng action lỗi để tăng maxMana và hồi mana. Kiểm tra toàn bộ điều kiện trước mutation; action bị từ chối phải giữ nguyên state, kể cả thứ tự tay bài.
2. **Spell sai target làm mất bài/mana.** `GameEngine.ts:57–94`, `EffectResolver.ts`: resolver có thể throw sau khi bài bị lấy khỏi tay và mana bị trừ; nhiều effect còn có thể áp dụng một phần. Paladin power cũng trừ mana trước khi summon vào board đầy. Cần validate trước và cơ chế commit nguyên tử cho nhiều effect.
3. **Có thể ghi đè kết quả trận.** `GameEngine.ts:173–175` gọi resign mà không kiểm tra PLAYING; `Game.ts` finish ghi đè winner. Người thua gửi CONCEDE sau trận có thể thay đổi kết quả tùy người gửi. Chặn mọi mutation sau FINISHED, finalization phải idempotent. `GameService.ts:244,358` còn lưu hai lần khi concede; upsert giảm trùng bản ghi nhưng vẫn có ghi thừa và cập nhật lại finishedAt.
4. **Lỗi khởi tạo trận không được bắt ở async boundary.** `GameService.ts:109–116` dùng void startGame và markStarted trước await DB. safe() trong SocketHandler chỉ bắt lỗi đồng bộ. Nếu truy vấn/khởi tạo thất bại, promise rejection không được xử lý và phòng đã khóa started. Dùng WAITING/STARTING/PLAYING, bắt lỗi async, trả lỗi phòng và phục hồi trạng thái cho phép thử lại.
5. **Reconnect ở màn hình game không tự chuyển sang kết quả.** `GameScreen.ts:77–104` cập nhật snapshot nhưng chỉ chuyển over khi nhận GAME_OVER; server reconnect chỉ gửi GAME_STATE_UPDATED, kể cả FINISHED. Nếu đối thủ kết thúc trận lúc người chơi mất mạng, reconnect có thể giữ màn game đã kết thúc. Route phải suy ra từ snapshot authoritative.
6. **Lobby có thể kẹt khi session hết hiệu lực.** `LobbyScreen.ts:35–37` không nghe ACTION_REJECTED, disconnect hoặc connect_error. Khi server restart, RECONNECT_FAILED không được xử lý ở lobby. Đưa listener vòng đời lên provider tồn tại xuyên màn hình.

### P2 — tính đúng đắn giao diện và vòng đời

- `GameScreen.ts:297–298` khóa click hero của mình dù ANY_CHARACTER cho phép chọn hero đó: spell không thể tự target hero. Kết nối lại handler và highlight theo tập target hợp lệ.
- `GameOverScreen.ts:30` “Chơi lại” chỉ reset store; singleton socket vẫn giữ token/phòng cũ. Khi reconnect sau đó có thể bị đưa lại trận cũ. Tách “Về trang chủ” (leave + clear session/socket auth) và “Tái đấu” (hai người cùng đồng ý, gameId mới).
- Hero selection chỉ cập nhật local trước server xác nhận; lobby không sử dụng DECK_SELECTED, snapshot reconnect chỉ chứa tên/id nên không phục hồi lựa chọn/ready. Bổ sung room snapshot authoritative.
- RoomManager không có cleanup, GameService không gọi removeGame, gameIndex không được dọn; phòng và game tích lũy theo số trận. Thêm TTL/grace period và xóa đồng bộ các index. Lookup token/socket hiện quét tất cả phòng.
- SocketHandler nhận TypeScript types nhưng chưa validate runtime; handleAction suy ra action từ field payload thay vì event. Dùng command phân biệt loại, validate tên/mã/id/hero và chỉ cho mỗi socket có một membership hoạt động.
- `minionToState()` gán maxHealth bằng currentHealth; cần giá trị maxHealth thật để hiển thị mất máu chính xác.
- CSS có overflow hidden toàn trang, message bar tối thiểu 420px, hero row không wrap và hàng điều khiển chứa cả tay bài; có nguy cơ cắt nội dung trên màn nhỏ. Breakpoint hiện chỉ đổi kích thước card/title.

### Quyết định luật cần ghi rõ khi triển khai

Hero power hiện dùng nhiều lần nếu đủ mana; tài liệu chưa giới hạn một lần/lượt nên chưa coi là bug. Fatigue, hòa khi cả hai hero chết, bộ bài theo class và mulligan cũng cần chốt luật riêng; không tự áp luật Hearthstone vào CoinCard. Deck hiện ưu tiên 30 lá rẻ là hành vi đã ghi trong GAME_RULES.

## 3. Kế hoạch triển khai theo thứ tự

### Bước 1 — bảo toàn state và kết quả

- Sửa validation/commit ở engine; thêm regression test board đầy, spell target sai, effect thứ hai thất bại, Paladin board đầy. So sánh toàn bộ state trước/sau action bị từ chối.
- Chặn concede sau kết thúc, finalization chỉ một lần; persistence có transaction và log lỗi để retry có kiểm soát.
- Bắt lỗi startGame async, khôi phục phòng khi thất bại; validate payload theo từng event.
- Điều kiện hoàn thành: các action lỗi không thay đổi state; không thể đổi winner sau kết thúc; lỗi DB khi start không làm phòng kẹt.

### Bước 2 — thống nhất logic chuyển màn hình và reconnect

Giữ phase ở client nhưng thay setPhase tùy ý bằng reducer/state machine. Tạo SessionController ở cấp provider để sở hữu socket listener; các màn hình chỉ hiển thị và gửi intent.

Luồng chính: BOOT → CONNECTING → HOME → JOINING/CREATING → LOBBY → STARTING → GAME → RESULTS.

| Sự kiện | Hành vi cần có |
| --- | --- |
| Mở trang có session | RECONNECTING, chờ room/game snapshot; không cho tạo phòng song song |
| ROOM snapshot | Phục hồi người chơi, hero, ready, trạng thái kết nối |
| Snapshot PLAYING | Vào GAME khi đủ session và state đúng gameId |
| Snapshot FINISHED | Vào RESULTS kể cả không nhận GAME_OVER |
| Mất mạng | Overlay RECONNECTING, vô hiệu action; không phát lại action cũ sau reconnect |
| Session không hợp lệ/phòng hết hạn | Clear session/auth, về HOME và giữ thông báo dễ hiểu |
| Rời lobby | LEAVE_ROOM có phản hồi server, cập nhật đối thủ và cleanup |
| Rời trận đang chơi | Dialog xác nhận đầu hàng; server xác nhận rồi điều hướng |
| Về trang chủ sau trận | Rời membership cũ và xóa auth trước khi cho tạo/join phòng mới |
| Tái đấu | Hai bên đồng ý → lựa chọn/ready → gameId mới; từ chối hoặc timeout vẫn ở RESULTS |

- Bổ sung requestId/actionId, response thành công/thất bại và stateVersion; bỏ response cũ sai session/game/request. Server xử lý actionId trùng một lần.
- Khóa submit trong lúc chờ; timeout có nút thử lại, reconcile snapshot trước retry action chưa rõ kết quả.
- Animation fade/translate khoảng 150–220ms, hỗ trợ reduced-motion. Không dùng setTimeout làm nguồn quyết định chuyển màn; animation chỉ theo sau state.
- Đối thủ mất mạng có thông báo và thời hạn reconnect do server quản lý. Thời hạn cụ thể là cấu hình, không đồng nhất disconnect với đầu hàng ngay.
- Điều kiện hoàn thành: refresh ở mỗi màn phục hồi đúng, server restart thoát lỗi đúng, không nhân đôi listener/room khi bấm nhanh, tái đấu không lẫn state cũ.

### Bước 3 — cải thiện giao diện và tương tác

- Home: form có label, trạng thái kết nối/loading, Enter submit, lỗi tiếng Việt gần input.
- Lobby: hai vị trí người chơi, nút sao chép mã phòng, mô tả hero power, lựa chọn được server xác nhận, trạng thái chờ và nút rời phòng.
- Game: tách vùng đối thủ / board / thông báo lượt / board mình / tay bài / thanh điều khiển; dùng grid thích ứng theo cả chiều rộng và chiều cao. Giữ HP, mana và End Turn luôn dễ nhìn.
- Tay bài tối đa 10 lá có layout co giãn và chế độ xem chi tiết bằng chạm/focus; không dựa hoàn toàn vào hover. Bàn đủ 7 minion mỗi bên vẫn đọc và chọn được.
- Nút chỉ sáng khi action hợp lệ; tự target hero, hủy chọn bằng Escape/nút hủy, thông báo thiếu mana/board đầy dễ hiểu. Âm thanh hành động theo xác nhận server; kết quả phát âm phù hợp thắng/thua.
- Results: kết quả, số lượt và tùy chọn về trang chủ/tái đấu; trạng thái chờ đối thủ tái đấu rõ ràng.
- Kiểm tra ở 360×800, 390×844, 768×1024, 1366×768 và 1920×1080; full hand/full board; bàn phím, touch, focus và reduced-motion.

### Bước 4 — tối ưu hiệu suất có số đo

- Đo baseline production bằng Performance/React Profiler: initial network, thời gian vào màn game, commit duration khi chọn bài và nhận snapshot, frame time animation. Đo server heap/số room/game sau nhiều vòng tạo–kết thúc–hết TTL.
- Ưu tiên cleanup/index trước: TTL phòng/game, Map token→room và socket→room; cập nhật index khi reconnect/leave. Kiểm tra không xóa session trong grace period.
- Audit asset thực sự được tải; tạo audio nén và ảnh theo kích thước dùng thực tế, preload có chọn lọc cho màn kế tiếp. Không preload toàn bộ 90,3 MiB. Audio manager có pool giới hạn, mute/volume và tái sử dụng thay new Audio mỗi click.
- Tách context session/connection khỏi state trận; tách Board/Hand/HUD và memo hóa nơi profiler chứng minh có render thừa. Callback và dữ liệu props cần ổn định để memo có tác dụng.
- Snapshot hiện gửi đầy đủ sau mỗi action và TURN_CHANGED cả khi lượt không đổi. Chỉ phát TURN_CHANGED khi thực sự đổi lượt; đo payload trước khi cân nhắc delta. Giữ full snapshot để resync, không vội thay protocol vì game chỉ có hai người và board nhỏ.
- Catalog hero/card có thể cache sau khi đo DB; version hóa/invalidate khi dữ liệu thay đổi. Ưu tiên asset và vòng đời hơn chia nhỏ JS ngay vì bundle gzip hiện khoảng 88 kB.
- Mục tiêu nghiệm thu đề xuất: phản hồi chọn bài dưới 100ms trên thiết bị kiểm thử đã ghi cấu hình; hướng tới frame time 16,7ms cho animation; heap và số room/game ổn định sau cleanup. Đây là mục tiêu, chưa phải kết quả đo.

### Bước 5 — kiểm thử luồng hoàn chỉnh và tài liệu

- Engine regression: lỗi không mutation, kết quả bất biến, start failure và idempotency.
- Socket integration: payload sai, hai người chọn cùng lúc, disconnect/reconnect ở lobby/game/results, phòng hết hạn, duplicate action, privacy tay bài.
- Browser E2E hai người: tạo/join → chọn hero → chơi → kết thúc → tái đấu/về home; reconnect sau khi đối thủ đã kết thúc; refresh và mạng chập chờn ở từng bước.
- Root build cần gồm shared + server + client và typecheck, tránh build xanh dù client có lỗi. Cập nhật SOCKET_PROTOCOL, GAME_RULES, ARCHITECTURE và README theo hành vi cuối cùng.

## 4. Cách chia đợt giao việc

1. Đợt A: engine correctness, finalization, async start và regression tests.
2. Đợt B: session controller/state machine, reconnect, leave và rematch protocol.
3. Đợt C: responsive UI, target interaction, transition và feedback.
4. Đợt D: profiling, asset/audio, render/network và kiểm chứng cleanup dưới tải.

Mỗi đợt có thể review riêng. B phụ thuộc nền ổn định của A; C dùng luồng của B; D cần so sánh baseline trước/sau, không khẳng định tăng hiệu suất chỉ từ thay đổi code.

## 5. Kế hoạch build thành local server để chơi online

Mục tiêu là chạy một máy làm server authoritative, các máy khác truy cập bằng trình duyệt. Có hai phạm vi triển khai:

- **LAN nội bộ:** phù hợp demo/lớp học; người chơi cùng Wi-Fi hoặc switch.
- **Internet:** máy server phải có địa chỉ công khai hoặc được đặt sau tunnel/port forwarding; chỉ nên mở sau khi đã có authentication, giới hạn tốc độ và HTTPS.

### Kiến trúc đích

```text
Browser A ─┐
Browser B ─┼─ HTTP/Vite static + Socket.IO ─ Local CoinCard Server ─ SQLite
Browser C ─┘                                  (authoritative game)
```

Trong bản đầu, server Node/Express phục vụ API health và Socket.IO; client React được build thành static files. SQLite vẫn đặt trên máy server và không chia sẻ trực tiếp qua mạng. Chỉ port HTTP/HTTPS được mở cho người chơi.

### Giai đoạn A — đóng gói server chạy ổn định

1. Thêm cấu hình môi trường rõ ràng: `HOST=0.0.0.0`, `PORT`, `CLIENT_ORIGIN`, `DATABASE_URL`, `SESSION_TTL`, `ROOM_TTL`, `NODE_ENV`.
2. Server phải bind `0.0.0.0`; client không hard-code `localhost`, mà lấy URL từ `VITE_SERVER_URL` hoặc dùng relative origin khi cùng một server phục vụ static.
3. Thêm `GET /health` trả trạng thái server, phiên bản, thời gian hoạt động và trạng thái database; không trả session/token.
4. Tạo script build/release duy nhất: build shared → generate Prisma → build server → build client → copy `client/dist` vào thư mục deploy.
5. Thêm xử lý graceful shutdown: ngừng nhận room mới, báo disconnect, đóng Socket.IO và Prisma, không làm hỏng database.
6. Ghi log có request/room/game id, không ghi session token hoặc toàn bộ hand.

Tiêu chí đạt: chạy được bằng một lệnh trên máy chủ mới, truy cập health từ máy khác trong LAN, không phụ thuộc Vite dev server.

### Giai đoạn B — chơi trong LAN

1. Cố định IP LAN của máy server hoặc đặt DHCP reservation, ví dụ `192.168.1.50`.
2. Cho phép inbound TCP trên port ứng dụng trong Windows Firewall; chỉ mở trên profile Private, không mở Public.
3. Build client với `VITE_SERVER_URL=http://192.168.1.50:3000` nếu static client chạy riêng; nếu Express phục vụ luôn client thì dùng relative URL để tránh CORS.
4. Cấu hình Socket.IO CORS chính xác theo danh sách origin của client, không dùng `*` khi có credential/session.
5. Kiểm tra từ hai thiết bị khác nhau: tạo phòng, join, chọn hero, chơi, refresh, disconnect/reconnect và kết thúc trận.
6. Hiển thị trên Home địa chỉ truy cập và trạng thái server cho người tổ chức; mã phòng vẫn là cơ chế mời người chơi.

### Giai đoạn C — mở chơi qua Internet

Ưu tiên theo thứ tự an toàn và dễ vận hành:

1. **Tunnel có HTTPS** cho demo nhanh: Cloudflare Tunnel hoặc công cụ tương đương trỏ vào port local. Client dùng URL HTTPS/WSS của tunnel; không cần mở router.
2. **VPS/reverse proxy** cho bản dùng thường xuyên: Nginx/Caddy nhận 443, cấp TLS, chuyển `/socket.io` và HTTP tới Node ở localhost. Socket.IO phải bật upgrade WebSocket và timeout đủ dài.
3. **Port forwarding** chỉ dùng khi bắt buộc: forward 80/443 tới máy server, không forward SQLite hoặc port database; cập nhật DDNS nếu IP nhà thay đổi.

Với HTTPS, toàn bộ client phải dùng `https://` và Socket.IO tự dùng `wss://`. Cấu hình origin theo domain, bật rate limit cho tạo/join room, giới hạn kích thước payload và timeout kết nối. Session token hiện lưu trong `sessionStorage`; cần thêm expiry, rotate khi reconnect và invalidate khi rời phòng.

### Giai đoạn D — dữ liệu và vòng đời phòng

- Chạy Prisma migration trước khi start; seed chỉ là bước cài đặt ban đầu, không chạy lại mỗi lần khởi động.
- Backup định kỳ file SQLite khi không có transaction đang ghi; với nhiều trận đồng thời, chuyển sang PostgreSQL.
- Thêm TTL cho room/game đã kết thúc, grace period cho disconnect và cleanup `RoomManager`, `gameIndex`, `GameEngine`.
- Ghi kết quả trận idempotent; shutdown/restart không được tạo winner hoặc game record trùng.
- Dùng volume/thư mục dữ liệu riêng trong deploy để cập nhật code không xóa database.

### Giai đoạn E — bảo mật tối thiểu trước khi chia sẻ link

- Validate runtime toàn bộ payload Socket.IO: tên, room code, hero id, game id, card/target id.
- Giới hạn tần suất CREATE_ROOM, JOIN_ROOM và action; từ chối payload quá lớn hoặc gửi quá nhanh.
- Không gửi hand đối thủ, session token trong broadcast, stack trace hoặc đường dẫn database.
- Chỉ nhận request từ origin đã cấu hình; dùng HTTPS khi qua Internet.
- Thêm cơ chế room host đóng phòng, người chơi rời phòng, và thông báo rõ khi server đầy/bảo trì.

### Giai đoạn F — kiểm thử phát hành

1. Local: `health`, migration, seed, restart, backup/restore.
2. LAN: hai laptop/điện thoại khác mạng Wi-Fi, kiểm tra firewall và địa chỉ IP.
3. Internet: hai mạng khác nhau, HTTPS/WSS, reconnect sau đổi mạng, refresh từng màn hình, duplicate action và timeout.
4. Tải nhỏ: 5–10 phòng đồng thời, theo dõi CPU, heap, số socket, payload và thời gian broadcast.
5. Chỉ phát hành khi toàn bộ E2E hiện tại và các luồng reconnect/room cleanup đạt; ghi lại lệnh rollback bản build và bản backup database.

### Cách chia bản phát hành

- **v0.1 LAN demo:** Node + Express static + Socket.IO + SQLite, IP nội bộ, không public Internet.
- **v0.2 tunnel demo:** HTTPS tunnel, origin/rate limit/session expiry, hai người chơi ngoài mạng.
- **v1 self-hosted online:** reverse proxy, domain, TLS tự động, backup, cleanup TTL, monitoring và quy trình cập nhật.

Phần nên làm trước trong code hiện tại là cấu hình host/origin, phục vụ `client/dist`, health check, graceful shutdown, runtime validation và cleanup phòng/game. Khi các phần này ổn định mới mở port hoặc tunnel; nếu mở Internet trước, lỗi reconnect và vòng đời session hiện tại sẽ khó chẩn đoán hơn.
