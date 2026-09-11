# Báo cáo kiểm thử CoinCard (bản final)

Ngày chạy: 2026-09-11. Môi trường: Windows, Node.js 24, database SQLite thật
(`data/coincard.db`) cho E2E; integration dùng database thử riêng biệt.

## Kết quả thực tế

| Nhóm | Lệnh | Kết quả |
| --- | --- | --- |
| Build + unit + integration | `npm run verify` | PASS — build shared/server/client, **31/31 unit**, integration (migrate + seed + catalog + start + lỗi lưu + session cũ + tái đấu + cleanup trên DB thử riêng) |
| Typecheck | `tsc --noEmit` từng workspace + trong `build` | PASS |
| E2E 2 người chơi | `node e2e-two-players.cjs` (server production `--serve-client`) | **16/16 pass** |
| Negative | `npm run test:e2e-neg` | **4/4 pass** (sai mã phòng, phòng đầy, game không tồn tại...) |
| Reconnect/mất mạng | `node e2e-connection.cjs` | PASS (join/start không race auth, đối thủ nhận reconnect, cả hai đánh tiếp được) |
| 2 tab trình duyệt | `node e2e-tabs.cjs` (server + Vite dev) | **6/6 pass** |
| Production một cổng | `GET /health`, `GET /` (không cần Vite) | `{"status":"ok"}`, HTTP 200 |

## Phạm vi unit (31 test)

- Action bị từ chối không mutation (mana/bài/board/thống kê giữ nguyên).
- Multi-effect lỗi hoàn tác toàn bộ action (checkpoint/rollback hai Player).
- Polymorph thay đúng slot khi board đầy, không tăng thống kê summon.
- DESTROY có/không ngưỡng minAttack; target dưới ngưỡng bị từ chối trước khi tiêu tài nguyên.
- Opening guarantee không nhân đôi bài khi deck không có lá rẻ (kiểm tra số lượng + instance duy nhất).
- Taunt bắt buộc mục tiêu; Charge đánh ngay; tay 6 lá burn khi đầy; rút thủ công 1 lần/lượt.
- Thắng/thua/hòa (cả hai hero chết cùng action → `winnerId null`).
- Thống kê damage thực (không overkill, tính cả phản công, không tính destroy/transform).
- Catalog thật: mọi spell resolve được trên target hợp lệ.

## Kiểm chứng tay (trên 1 máy, 2 tab)

- Tạo/join phòng → chọn hero → đánh bài/tấn công/rút bài → kết thúc → tái đấu: đạt.
- Refresh ở lobby/game/results, disconnect/reconnect, che tay đối thủ: đạt.
- Màn hẹp: cuộn dọc, tay bài cuộn ngang, các nút tiếp cận được.
- Ảnh/âm thanh load; layout desktop đầy đủ; mobile ở mức dùng được.

## Giới hạn đã biết

- Kiểm thử tự động chạy nội bộ (localhost); kiểm chứng qua Internet trên hai thiết bị khác mạng thực hiện riêng qua Quick Tunnel, không import phòng đang chơi làm dữ liệu test.
- Restart server không phục hồi trận đang chơi; lưu kết quả lỗi retry tối đa 2 lần rồi ghi log (chưa có durable queue qua crash).
