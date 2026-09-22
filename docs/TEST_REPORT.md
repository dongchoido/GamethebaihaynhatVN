# Báo cáo kiểm thử CoinCard

Ngày cập nhật: 2026-09-22. Nghiệm thu gần nhất chạy bằng Eclipse Temurin JDK 17.
Không hardcode số lượng test trong tài liệu; Maven và Vitest output là nguồn kết quả
chính thức của mỗi lần chạy.

| Nhóm | Lệnh | Kết quả gần nhất |
| --- | --- | --- |
| Java unit, application, database, static analysis | `server-java/mvnw verify` | PASS (JDK 17) |
| Shared build, frontend typecheck và component tests | `npm run build:shared`, `npm run typecheck --workspace client`, `npm run test --workspace client` | PASS |
| Production build | `npm run build` | PASS |
| Full gate trên database và cổng tạm | `npm run verify` | PASS |

## Phạm vi tự động

- Deck 30 lá, class legality, collectible-only, copy limit, auto-build và token rejection.
- Opening hand, Coin, mana, hand limit/burn, fatigue tăng dần và hero power một lần mỗi lượt.
- Charge, Taunt, combat đồng thời, spell chọn hero bằng `playerId`, rollback và kết quả hòa.
- Per-game command lock, room/loadout/start, reconnect, persistence, cleanup và rematch.
- WebSocket typed payload, event cũ/unknown, malformed payload, session ownership và
  viewer-specific hand.
- E2E dùng Node WebSocket clients kết nối vào Spring Boot production JAR với catalog
  CoinCard thật cho luồng hai người chơi; fixture catalog riêng cho Taunt, hero target,
  fatigue, game over và rematch. Đây không phải browser UI automation.

## Quality Gate

`npm run verify` chạy theo thứ tự: shared build, frontend typecheck, frontend tests,
Maven verify (JaCoCo domain/application, Checkstyle, SpotBugs), production build và E2E.
E2E tự tạo SQLite database cùng port tạm và luôn shutdown server sau khi chạy.

JaCoCo có hai `BUNDLE` check độc lập: domain (`game` và `model`) và application.
Mỗi check lọc class ở configuration riêng và yêu cầu tối thiểu 80% line, 70% branch.
Một lần chạy có chủ ý với application line threshold 100% đã fail, xác nhận gate được áp dụng.

## Giới hạn chủ ý

- E2E chạy localhost; restart server không khôi phục game đang chơi.
- Không có mulligan, account, AI hoặc lưu trận đang chơi qua restart.
- Catalog CoinCard không mô phỏng toàn bộ Hearthstone thật.
