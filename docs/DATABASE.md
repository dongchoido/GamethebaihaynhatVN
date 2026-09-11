# Database

SQLite + Prisma 6. DATABASE_URL trong server/.env: file:../data/coincard.db resolve thành server/data/coincard.db theo vị trí schema.

Cài mới: tạo .env, npm run prisma:generate, npm run prisma:migrate, npm run seed. Seed upsert 34 cards/5 heroes, không tự chạy khi build/start.

Nâng cấp: dừng server, sao lưu DB, chạy npm run prisma:migrate. Migration 20260911120000_allow_rematches chỉ bỏ unique index Game.roomCode và thêm index thường; giữ mọi dòng dữ liệu. Cùng phòng có thể chứa nhiều trận tái đấu.

Game, Player, GamePlayer được upsert trong transaction. Hòa có winnerId null và cả hai winner=false. Số liệu damage/cardsPlayed/minionsSummoned hiện ở RAM. GameAction/GameHistory/Deck/DeckCard có trong schema nhưng chưa ghi replay/deck trong luồng chơi. startedAt là thời điểm tạo bản ghi kết quả, chưa phải thời điểm bắt đầu thực.

npm run test:integration tạo server/data/.integration-* với SQLite riêng; migrate/seed chỉ vào DB thử. Giữ DB thử để điều tra, *.db được gitignore. --keep giữ server cho browser QA. Không sửa DB cá nhân.

Lưu lỗi retry sau 1 và 2 giây rồi log; chưa có durable queue hay khôi phục trận sau crash.

