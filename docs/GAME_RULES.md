# Luật CoinCard

- Hai hero 30 HP. Một hero về 0 thì thua, cả hai về 0 trong cùng action thì hòa. Đầu hàng kết thúc ngay; không đổi kết quả sau khi kết thúc.
- Deck 30 lá collectible rẻ nhất trong catalog, không lọc class, shuffle phía server. Catalog hiện có 34 lá; Sheep/Recruit là token.
- Tay mở đầu 3/4 lá. Đổi lá đắt nhất lấy lá ≤2 mana nếu deck có, không nhân đôi bài. Không bảo đảm có minion chơi được ở mana 1.
- Mỗi lượt riêng tăng max mana 1 (tối đa 10), hồi đầy và tự rút 1. Người đi trước không rút thêm ở lượt mở đầu.
- Tay tối đa **6 lá**, bàn tối đa **7 minion mỗi bên**. Tự rút/effect khi tay đầy làm cháy lá vừa rút.
- Rút thủ công thêm **một lần/lượt**, không tốn mana. Tay đầy hoặc deck hết thì từ chối, không mất bài. Refresh giữ giới hạn. Chưa có fatigue.
- Minion mới triệu hồi không đánh ngay, trừ CHARGE. Mỗi minion đánh một lần/lượt. TAUNT bắt buộc mục tiêu tấn công, không áp dụng spell/power.
- Minion giao chiến gây sát thương đồng thời. Damage thống kê là HP thực mất, kể cả phản công; không tính overkill hoặc tự gây damage.
- Polymorph thay đúng slot bằng Sheep 1/1 kể cả bàn đầy, không tăng thống kê triệu hồi.
- DESTROY chỉ xét ngưỡng khi có minAttack. Siphon Soul phá minion bất kỳ và hồi 3 hero mình; Shadow Word: Death yêu cầu công ≥5.
- Action bị từ chối không mất tài nguyên. Effect sau lỗi sẽ hoàn tác cả action.
- Hero power dùng nhiều lần/lượt nếu đủ mana.

| Hero | Power (2 mana) |
| --- | --- |
| Mage | 1 damage hero địch |
| Hunter | 2 damage hero địch |
| Paladin | Recruit 1/1; bàn đầy từ chối không mất mana |
| Priest | Hồi 2 hero mình, tối đa 30 |
| Warlock | Tự mất 2 HP và rút 1; hết deck vẫn mất HP, tay đầy cháy lá |

Effect chi tiết ở data/cards.json. Tái đấu cần hai phiếu, tạo gameId mới cùng phòng/hero và chia lại bài.

