# Báo cáo kiểm thử CoinCard

Ngày cập nhật: 2026-09-21. Backend duy nhất: Spring Boot + WebSocket + SQLite.

| Nhóm | Lệnh | Kết quả gần nhất |
| --- | --- | --- |
| Java unit/regression/database | `npm run test:java` | **22/22 pass** |
| Client typecheck + production build | `npm run build:client` | PASS |
| E2E hai người | `npm run test:e2e` | **16/16 pass** |
| Negative | `npm run test:e2e-neg` | **4/4 pass** |
| Reconnect | `npm run test:e2e-connection` | PASS |
| Hai tab qua Vite | `npm run test:e2e-tabs` | **6/6 pass** |
| Production một cổng | `GET /health`, `GET /` | HTTP 200 |

## Phạm vi Java

- Validate trước mutation và rollback toàn bộ multi-effect lỗi.
- Toàn bộ spell trong catalog thật resolve với target hợp lệ.
- Polymorph trên board đầy; destroy có điều kiện; Siphon Soul.
- Charge, Taunt, minion chắn hero, hand/board limit và manual draw.
- Thắng, thua, hòa khi hai hero chết cùng action.
- Damage thực không tính overkill và có tính phản công.
- Persistence transaction và nhiều rematch trong cùng room.

## Giới hạn

- E2E chạy trên localhost; Quick Tunnel được kiểm tra vận hành riêng.
- Restart server không phục hồi game đang chơi.
- Retry lưu kết quả chưa phải durable queue.
