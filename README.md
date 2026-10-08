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
- Nền lobby/bàn đấu, ảnh Hero theo deck và tranh minh họa trên các lá bài.
- Tinh thể mana hiển thị mana còn lại và mana đã dùng.
- Âm thanh khi mở game, chọn deck, bắt đầu trận, đánh bài, tấn công, kết thúc lượt và chiến thắng.
- Nút bật/tắt âm thanh ở lobby và bàn đấu, áp dụng cả tiếng báo lỗi và giữ lựa chọn khi chuyển màn hình.

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

Hoặc biên dịch trực tiếp bằng JDK trong PowerShell, tại thư mục project:

```powershell
New-Item -ItemType Directory -Path out -Force | Out-Null
$sources = @(Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac --release 17 -encoding UTF-8 -d out $sources
Copy-Item -Path src/main/resources/* -Destination out -Recurse -Force
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

src/main/resources
├── images       design, Heros, HeroPower, Minions
└── sounds       Các hiệu ứng WAV
```

## Ảnh và âm thanh

Tài nguyên được đóng gói trong project, không cần thư mục ảnh/âm thanh bên ngoài.
Maven và NetBeans tự đưa `src/main/resources` vào classpath. Chỉ dùng Java `ImageIO`
và `javax.sound.sampled`, không thêm thư viện.

`GameAssets` nạp/cache ảnh và ánh xạ mã bài sang tranh minh họa. Vì ảnh gốc đã có
tên/chỉ số in sẵn, giao diện chỉ lấy phần tranh; tên, mana, sát thương và máu vẫn
hiển thị từ dữ liệu game. Có thể đổi ảnh của từng bài trong bảng `CARD_ART`.

`SoundPlayer` phát hiệu ứng trên luồng riêng. Nếu tắt âm thanh hoặc máy không có
thiết bị âm thanh, trận đấu vẫn chạy bình thường. Các ảnh HeroPower được giữ trong
resources để dùng sau; game hiện tại chưa có luật Hero Power.

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
- Minion phải hạ hết Minion đối phương trên bàn trước khi tấn công Hero.
- Minion vừa được triệu hồi vẫn bảo vệ Hero, dù chưa thể tấn công.
- Damage Spell gây sát thương trực tiếp lên Hero đối phương.
- Heal Spell hồi máu cho Hero hiện tại.
- Tay bài tối đa 7 lá. Khi tay đã đầy, lá được rút tiếp theo bị bỏ.
- Chỉ thua vì hết bài khi cả deck và bài trên tay đều hết, kể cả khi còn Minion trên bàn.
- Deck trống nhưng tay còn bài, hoặc tay trống nhưng deck còn bài, thì vẫn tiếp tục chơi.
- Rút từ deck trống không gây mất máu.
- Lá cuối vẫn được xử lý hiệu ứng trước khi kiểm tra hết bài. Nếu lá đó hạ Hero đối phương, người đánh lá bài vẫn thắng.
- Mỗi deck cần có ít nhất 1 lá để bắt đầu trận; deck rỗng là cấu hình không hợp lệ.
- Hero về 0 máu thì thua.

## Kiểm tra tự động

Trong PowerShell, tại thư mục project (JDK 17+ có trong PATH):

```powershell
.\run-tests.ps1
```

Script biên dịch toàn bộ code và chạy kiểm tra luật chơi, phòng thủ, hết bài,
giới hạn 7 lá, giao diện kết thúc trận và lưu dữ liệu. Dữ liệu thử được tạo riêng,
không ghi vào thư mục `data` của người chơi.

`ExhaustionSmokeTest` kiểm tra 18 nhóm tình huống, bao gồm 256 trận mô phỏng
với kích thước deck khác nhau. `EndGameSmokeTest` cần môi trường desktop để tạo
Swing frame ẩn; kiểm tra khóa nút sau trận, lý do kết thúc và tránh lưu trùng.
