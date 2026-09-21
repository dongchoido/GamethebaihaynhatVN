# Database

Backend dùng SQLite qua Spring JDBC. Mặc định database nằm tại
`data/coincard.db`; có thể đổi bằng `COINCARD_DB_PATH`.
`DATABASE_URL` cũng được chấp nhận để tương thích môi trường triển khai hiện có.

Khi khởi động, `DatabaseSeeder`:

1. Tạo các bảng `Card`, `Hero`, `Game`, `Player`, `GamePlayer` nếu chưa có.
2. Tạo index thường cho `Game.roomCode` để một phòng lưu được nhiều trận rematch.
3. Upsert catalog từ `data/cards.json` và năm hero.

Không cần chạy migration hoặc seed thủ công. `Card`/`Hero` là catalog;
`Game`/`Player`/`GamePlayer` lưu kết quả. Một trận hòa có `winnerId = null` và
hai dòng người chơi đều `winner = 0`.

Ghi kết quả chạy trong transaction. Nếu SQLite lỗi, service thử lại sau 1 và 2
giây rồi ghi log. Game đang chơi và thống kê realtime nằm trong RAM, chưa có
durable queue hoặc phục hồi trận sau khi process dừng.

`JdbcRepositoriesTest` dùng database tạm và xác minh hai trận rematch cùng room
được lưu độc lập. File `*.db` bị gitignore và không thuộc source bài nộp.
