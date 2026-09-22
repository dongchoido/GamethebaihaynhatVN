# Luật CoinCard

## Deck và khởi đầu

- Mỗi người có một hero 30 HP và deck đúng 30 lá.
- Chỉ lá `collectible=true` được đưa vào deck; lá phải là `NEUTRAL` hoặc đúng
  hero class. Lá thường tối đa 2 bản, Legendary tối đa 1 bản.
- Người đi trước được chọn ngẫu nhiên, nhận 3 lá và tự rút 1 lá khi bắt đầu lượt
  đầu. Người đi sau nhận 4 lá và thêm The Coin, sau đó tự rút khi đến lượt.
- Không có mulligan. Deck được shuffle phía server.

## Lượt và tài nguyên

- Đầu lượt: tăng max mana tối đa 10, hồi đầy mana, reset hero power và tự rút 1.
- Tay tối đa 10 lá. Rút khi tay đầy sẽ burn lá vừa rút.
- Deck rỗng gây fatigue tăng dần 1, 2, 3... damage cho hero ở mỗi lần rút thất bại.
- The Coin là spell token không collectible, cho 1 temporary mana trong lượt và
  không làm tổng mana dùng được vượt 10.
- Hero power chỉ dùng một lần mỗi lượt. Paladin hero power từ chối khi board đầy.

## Bàn và chiến đấu

- Bàn tối đa 7 minion mỗi bên.
- Minion mới không được đánh ngay, trừ minion có `CHARGE`.
- Mỗi minion chỉ đánh một lần trong lượt.
- Taunt là luật chặn duy nhất: nếu đối thủ có Taunt, attacker phải chọn một
  Taunt. Nếu không có Taunt, attacker có thể chọn hero hoặc minion bất kỳ.
- Minion giao chiến gây sát thương đồng thời. Damage thống kê là HP thực mất,
  không tính overkill.
- Mọi spell, hero power, fatigue và combat đều kiểm tra kết quả ngay trong cùng
  command. Hai hero chết trong cùng command là hòa.

## Effect và rollback

- Effect được resolve qua `CardEffect` strategy registry. Target được biểu diễn
  bằng `GameCharacter` (`Hero` hoặc `Minion`), không dùng `Object` trong effect core.
- Wire target cho hero luôn là `playerId`; wire target cho minion là `instanceId`.
  Vì vậy spell `ENEMY_CHARACTER` có thể đánh trực tiếp hero bằng id người chơi.
- Command nhiều effect tạo `GameMemento` trước commit. Nếu một effect lỗi, hand,
  mana, board, deck, hero health và thống kê được khôi phục.
- Catalog CoinCard là nguồn behavior của game; không mô phỏng toàn bộ Hearthstone.

## Kết thúc và tái đấu

- Hero về 0 HP thì game kết thúc; đầu hàng trao thắng cho đối thủ.
- Hai người cùng vote rematch thì server validate lại loadout đã chọn, shuffle deck
  mới, chọn người đi trước mới và bắt đầu game mới.
