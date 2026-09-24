# Hearthstone Swing

Game thẻ bài offline được viết hoàn toàn bằng Java và Java Swing.

Project **không dùng**:

- Spring Boot.
- HTML, CSS hoặc JavaScript.
- Web server.
- Database server hoặc thư viện bên ngoài.

## Chức năng

- Lobby nhập tên hai người chơi.
- Chọn một trong hai deck mẫu.
- Ba loại bài: Minion, Damage Spell, Heal Spell.
- Chơi turn-based trên cùng một máy.
- Mana tăng theo lượt, tối đa 10.
- Rút bài, đánh bài, triệu hồi Minion.
- Minion tấn công Hero hoặc Minion đối phương.
- Damage, Health và Win/Lose.
- Lưu User, Deck đã chọn và lịch sử Match trong thư mục `data`.
- Giao diện Swing có hiệu ứng hover, chọn lá bài và thông báo chuyển lượt.

## Yêu cầu

- JDK 17 trở lên.
- Apache NetBeans.

## Mở bằng NetBeans

1. Giải nén project.
2. Trong NetBeans chọn `File -> Open Project`.
3. Chọn thư mục `HearthstoneSwing` chứa file `pom.xml`.
4. Mở `Source Packages -> hearthstone -> Main.java`.
5. Chuột phải `Main.java` -> `Run File`.

Project này không có parent POM và không có runtime dependency, nên không gặp lỗi tải Spring Boot.

## Chạy bằng terminal

Nếu có Maven:

```bash
mvn clean compile exec:java
```

Hoặc biên dịch trực tiếp bằng JDK:

```bash
javac -d out src/main/java/hearthstone/**/*.java
java -cp out hearthstone.Main
```

## Cấu trúc

```text
src/main/java/hearthstone
├── Main.java
├── engine       Luật chơi
├── model
│   ├── card     Card, MinionCard, SpellCard
│   └── game     Hero, Player, GameState
├── persistence  Lưu User, Deck, Match ra file TSV
├── service      DeckCatalog
└── ui           LobbyFrame, GameFrame, CardButton
```

## Dữ liệu lưu ở đâu?

Sau khi chạy, project tự tạo:

```text
data/
├── users.tsv
├── decks.tsv
└── matches.tsv
```

Các file này có thể mở bằng Notepad hoặc Excel. Khi chuyển sang MySQL sau này,
chỉ cần thay lớp `FileGameRepository`; game engine và Swing UI không phải viết lại.

## Luật MVP

- Mỗi Hero bắt đầu với 30 máu.
- Mỗi người rút 3 lá; người chơi 1 đi trước.
- Minion vừa được triệu hồi chưa thể tấn công ngay.
- Damage Spell gây sát thương trực tiếp lên Hero đối phương.
- Heal Spell hồi máu cho Hero hiện tại.
- Bộ bài hết thì mỗi lần rút sẽ mất 1 máu.
- Hero về 0 máu thì thua.
